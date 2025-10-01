import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: 'breadcrumb.dashboard', icon: 'Odometer' },
        },
        // Solver
        {
          path: 'solver',
          name: 'solver',
          redirect: '/solver/problems',
          meta: { title: 'breadcrumb.solver', icon: 'Cpu' },
          children: [
            {
              path: 'problems',
              name: 'solver-problems',
              component: () => import('@/views/solver/ProblemList.vue'),
              meta: { title: 'breadcrumb.problems' },
            },
            {
              path: 'create',
              name: 'solver-create',
              component: () => import('@/views/solver/ProblemCreate.vue'),
              meta: { title: 'breadcrumb.createProblem' },
            },
            {
              path: 'detail/:id',
              name: 'solver-detail',
              component: () => import('@/views/solver/ProblemDetail.vue'),
              meta: { title: 'breadcrumb.problemDetail' },
            },
            {
              path: 'algorithms',
              name: 'solver-algorithms',
              component: () => import('@/views/solver/AlgorithmList.vue'),
              meta: { title: 'breadcrumb.algorithms' },
            },
          ],
        },
        // Orchestrator
        {
          path: 'orchestrator',
          name: 'orchestrator',
          redirect: '/orchestrator/flows',
          meta: { title: 'breadcrumb.orchestrator', icon: 'Connection' },
          children: [
            {
              path: 'flows',
              name: 'orchestrator-flows',
              component: () => import('@/views/orchestrator/FlowList.vue'),
              meta: { title: 'breadcrumb.flows' },
            },
            {
              path: 'editor/:id?',
              name: 'orchestrator-editor',
              component: () => import('@/views/orchestrator/FlowEditor.vue'),
              meta: { title: 'breadcrumb.flowEditor' },
            },
            {
              path: 'executions',
              name: 'orchestrator-executions',
              component: () => import('@/views/orchestrator/FlowExecution.vue'),
              meta: { title: 'breadcrumb.executions' },
            },
          ],
        },
        // Data
        {
          path: 'data',
          name: 'data',
          redirect: '/data/sources',
          meta: { title: 'breadcrumb.data', icon: 'DataAnalysis' },
          children: [
            {
              path: 'sources',
              name: 'data-sources',
              component: () => import('@/views/data/SourceList.vue'),
              meta: { title: 'breadcrumb.sources' },
            },
            {
              path: 'sources/create',
              name: 'data-source-create',
              component: () => import('@/views/data/SourceCreate.vue'),
              meta: { title: 'breadcrumb.createSource' },
            },
            {
              path: 'pipelines',
              name: 'data-pipelines',
              component: () => import('@/views/data/PipelineList.vue'),
              meta: { title: 'breadcrumb.pipelines' },
            },
            {
              path: 'pipelines/editor/:id?',
              name: 'data-pipeline-editor',
              component: () => import('@/views/data/PipelineEditor.vue'),
              meta: { title: 'breadcrumb.pipelineEditor' },
            },
            {
              path: 'apis',
              name: 'data-apis',
              component: () => import('@/views/data/ApiList.vue'),
              meta: { title: 'breadcrumb.apis' },
            },
            {
              path: 'query',
              name: 'data-query',
              component: () => import('@/views/data/QueryExplorer.vue'),
              meta: { title: 'breadcrumb.queryExplorer' },
            },
          ],
        },
        // Admin
        {
          path: 'admin',
          name: 'admin',
          redirect: '/admin/users',
          meta: { title: 'breadcrumb.admin', icon: 'Setting' },
          children: [
            {
              path: 'users',
              name: 'admin-users',
              component: () => import('@/views/admin/UserList.vue'),
              meta: { title: 'breadcrumb.users' },
            },
            {
              path: 'roles',
              name: 'admin-roles',
              component: () => import('@/views/admin/RoleList.vue'),
              meta: { title: 'breadcrumb.roles' },
            },
            {
              path: 'audit',
              name: 'admin-audit',
              component: () => import('@/views/admin/AuditLog.vue'),
              meta: { title: 'breadcrumb.auditLog' },
            },
            {
              path: 'workspaces',
              name: 'admin-workspaces',
              component: () => import('@/views/admin/WorkspaceList.vue'),
              meta: { title: 'breadcrumb.workspaces' },
            },
            {
              path: 'system',
              name: 'admin-system',
              component: () => import('@/views/admin/SystemInfo.vue'),
              meta: { title: 'breadcrumb.system' },
            },
          ],
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard',
    },
  ],
})

/** Map a route path to its top-level menu key for permission checks. */
function getMenuKeyFromPath(path: string): string | null {
  const segments = path.split('/').filter(Boolean)
  if (segments.length === 0) return null
  const topLevel = segments[0]
  // These are the controllable menu modules
  if (['solver', 'orchestrator', 'data', 'admin'].includes(topLevel)) {
    return topLevel
  }
  return null // dashboard and unknown paths are always allowed
}

router.beforeEach((to, _from, next) => {
  if (to.meta.public) {
    next()
    return
  }
  const userStore = useUserStore()
  if (!userStore.token) {
    next('/login')
    return
  }

  // Check menu permission
  const menuKey = getMenuKeyFromPath(to.path)
  if (menuKey && !userStore.hasMenu(menuKey)) {
    next('/dashboard')
    return
  }

  next()
})

export default router
