# LabWaypoint

LabWaypoint 是面向实验室的工作平台，目前提供设备档案、借还协作和使用教程。比赛发现与装备推荐是后续规划的功能。

## 项目现状

- `backend/`：Spring Boot 后端，提供用户、注册审核、设备、借还、教程、编辑授权和图片上传接口。
- `frontend/`：使用 Vue 3 和 Vite 重写的前端，视觉风格参考苏联构成主义海报。
- 比赛发现与推荐尚无后端接口，前端也没有填充虚构数据。

## 启动后端

需要 Java 17、Maven 和 MySQL。将 `backend/src/main/resources/application.yml.example` 复制为同目录下的 `application.yml`，并通过环境变量配置：

- `DB_PASSWORD`：本机数据库密码。
- `JWT_SECRET`：至少 32 个字符的随机私钥。
- `UPLOAD_PATH`：可选；不设置时文件写入 `./uploads/`。

请勿把实际密码或私钥提交到 Git。后端目前使用已有的 `lab_equipment` 数据库；`backend/sql/` 提供借还和注册审核的迁移脚本，尚未提供从空库创建完整数据库的脚本。

配置完成后，在 `backend/` 目录运行：

```sh
mvn spring-boot:run
```

Windows 环境下，后端会使用 Tomcat NIO2 连接器，以避开本机 Java NIO 回环连接启动失败的问题。

## 启动前端

先启动后端，再在 `frontend/` 目录运行：

```sh
npm ci
npm run dev
```

开发服务器默认使用 5173 端口，并将 `/api` 和 `/uploads` 代理到 8080 端口的后端。前端使用哈希路由，首页地址为 `http://127.0.0.1:5173/#/home`。

运行 `npm run build` 会生成 `frontend/dist/`；构建结果不会自动复制到后端的 `static/` 目录。前端的更多配置见 [前端说明](frontend/README.md)。

## 目录说明

- `backend/src/main/java/`：后端接口与业务逻辑。
- `backend/src/main/resources/application.yml.example`：不含实际密钥的配置模板。
- `backend/sql/`：现有数据库迁移脚本。
- `frontend/src/`：Vue 页面、接口客户端、登录状态与样式。

本地凭据、数据库备份、上传文件、构建产物和旧版前端打包文件均不纳入 Git。
