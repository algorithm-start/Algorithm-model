package com.recplatform.iam.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkspaceVO {

    private Long id;

    private String name;

    private String code;

    private String description;

    private Long ownerId;

    private String ownerName;

    private Integer memberCount;

    private String myRole;

    private LocalDateTime createTime;

    private List<MemberVO> members;

    @Data
    public static class MemberVO {
        private Long id;
        private Long userId;
        private String username;
        private String nickname;
        private String role;
        private LocalDateTime createTime;
    }
}
