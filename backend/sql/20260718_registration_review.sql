-- 2026-07-18 注册审核、学号与专业迁移
-- 执行前请备份 lab_equipment 数据库；本脚本针对当前生产表结构执行一次。

ALTER TABLE `user`
    ADD COLUMN `student_no` VARCHAR(32) NULL COMMENT '学号（业务身份标识）' AFTER `username`,
    ADD COLUMN `major` VARCHAR(100) NULL COMMENT '专业' AFTER `student_no`,
    ADD COLUMN `registration_status` ENUM('待审核', '已通过', '已拒绝') NOT NULL DEFAULT '已通过' COMMENT '注册审核状态' AFTER `role`,
    ADD COLUMN `registration_reviewed_by` INT NULL COMMENT '审核人用户ID' AFTER `registration_status`,
    ADD COLUMN `registration_reviewed_at` DATETIME NULL COMMENT '审核时间' AFTER `registration_reviewed_by`,
    ADD COLUMN `registration_reject_reason` VARCHAR(500) NULL COMMENT '拒绝原因' AFTER `registration_reviewed_at`,
    ADD UNIQUE KEY `uk_user_student_no` (`student_no`),
    ADD KEY `idx_user_registration_status` (`registration_status`);

-- 本次迁移前已经存在的账号视为已审核通过，避免原账号被锁定。
UPDATE `user`
SET `registration_status` = '已通过',
    `registration_reject_reason` = NULL
WHERE `registration_status` IS NULL OR `registration_status` <> '已通过';