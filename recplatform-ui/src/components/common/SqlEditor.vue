<template>
  <div ref="editorHost" class="sql-editor"></div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, shallowRef } from 'vue'
import { EditorState, Compartment } from '@codemirror/state'
import { EditorView, keymap, placeholder as cmPlaceholder } from '@codemirror/view'
import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import {
  autocompletion,
  completionKeymap,
  acceptCompletion,
} from '@codemirror/autocomplete'
import { sql, MySQL, type SQLNamespace } from '@codemirror/lang-sql'

/**
 * Schema shape consumed by the editor for table/column completion.
 * Keys are table names; values are their column names.
 */
export type SqlSchema = Record<string, string[]>

const props = withDefaults(
  defineProps<{
    modelValue: string
    schema?: SqlSchema
    placeholder?: string
    minHeight?: string
  }>(),
  {
    schema: () => ({}),
    placeholder: 'SELECT * FROM table_name WHERE ...',
    minHeight: '260px',
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const editorHost = ref<HTMLElement>()
const view = shallowRef<EditorView>()

// The SQL dialect (and thus its schema) is reconfigurable so we can refresh
// table/column completions when the selected data source changes.
const sqlCompartment = new Compartment()

function buildSqlExtension() {
  const schema = props.schema as SQLNamespace
  return sql({
    dialect: MySQL,
    schema,
    // Surface table/column names even before typing a dot.
    upperCaseKeywords: false,
  })
}

/**
 * Tab accepts the active completion when the popup is open; otherwise it
 * inserts an indent. This gives the "tab to complete" behaviour the user asked
 * for without breaking normal indentation.
 */
const tabCompletionKeymap = keymap.of([
  {
    key: 'Tab',
    run: (target) => acceptCompletion(target),
  },
  indentWithTab,
])

onMounted(() => {
  if (!editorHost.value) return

  const updateListener = EditorView.updateListener.of((update) => {
    if (update.docChanged) {
      const text = update.state.doc.toString()
      if (text !== props.modelValue) emit('update:modelValue', text)
    }
  })

  const state = EditorState.create({
    doc: props.modelValue,
    extensions: [
      history(),
      autocompletion({ activateOnTyping: true, icons: true }),
      tabCompletionKeymap,
      keymap.of([...defaultKeymap, ...historyKeymap, ...completionKeymap]),
      cmPlaceholder(props.placeholder),
      EditorView.lineWrapping,
      EditorView.theme({
        '&': { fontSize: '13px', minHeight: props.minHeight },
        '.cm-content': {
          fontFamily: "'JetBrains Mono', 'Fira Code', Menlo, Consolas, monospace",
        },
        '.cm-scroller': { overflow: 'auto' },
        '&.cm-focused': { outline: 'none' },
      }),
      sqlCompartment.of(buildSqlExtension()),
      updateListener,
    ],
  })

  view.value = new EditorView({ state, parent: editorHost.value })
})

onBeforeUnmount(() => {
  view.value?.destroy()
})

// Keep the editor doc in sync when the model is changed externally.
watch(
  () => props.modelValue,
  (value) => {
    const currentView = view.value
    if (!currentView) return
    const current = currentView.state.doc.toString()
    if (value !== current) {
      currentView.dispatch({
        changes: { from: 0, to: current.length, insert: value ?? '' },
      })
    }
  },
)

// Reconfigure completions when the schema (data source) changes.
watch(
  () => props.schema,
  () => {
    const currentView = view.value
    if (!currentView) return
    currentView.dispatch({
      effects: sqlCompartment.reconfigure(buildSqlExtension()),
    })
  },
  { deep: true },
)
</script>

<style scoped>
.sql-editor {
  width: 100%;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}
.sql-editor :deep(.cm-editor) {
  max-height: 420px;
}
.sql-editor :deep(.cm-editor.cm-focused) {
  border-color: var(--el-color-primary);
}
</style>
