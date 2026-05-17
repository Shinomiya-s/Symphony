# Symphony

> An AI-powered collaborative learning platform for students.

Symphony is a full-stack web application that combines real-time collaboration, intelligent mistake tracking, and AI-driven insights to help students learn together more effectively.

------

## Screenshots

### Home

![屏幕截图 2026-04-26 143803](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\屏幕截图 2026-04-26 143803.png)

### Login

![登录注册](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\登录注册.png)

### Study Partner Chat

![微信图片_20260414231722_606_22](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\微信图片_20260414231722_606_22.jpg)

### Mistake Archive

![屏幕截图 2026-04-26 145429](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\屏幕截图 2026-04-26 145429.png)

### AI Analysis & Practice Problems

![微信图片_20260414215459_593_22](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\微信图片_20260414215459_593_22.jpg)

### Knowledge Garden

![屏幕截图 2026-03-21 233010](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\屏幕截图 2026-03-21 233010.png)

### Knowledge Entry Detail

![屏幕截图 2026-03-21 233103](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\屏幕截图 2026-03-21 233103.png)

### Collaborative Document

![屏幕截图 2026-04-26 144834](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\屏幕截图 2026-04-26 144834.png)

### Document Archive

![屏幕截图 2026-04-26 144844](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\屏幕截图 2026-04-26 144844.png)

### Daily AI Report

![微信图片_20260414220008_596_22](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\微信图片_20260414220008_596_22.jpg)

### Personal AI Dashboard

![屏幕截图 2026-05-11 220536](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\屏幕截图 2026-05-11 220536.png)

### Drawing Whiteboard

![屏幕截图 2026-04-26 150107](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\屏幕截图 2026-04-26 150107.png)

------

## Features

**Core**

- Study partner chat with real-time messaging
- Public room chat with Redis-backed caching
- Personal AI assistant (floating widget, customizable per user)
- Real-time collaborative documents with Markdown support and AI summarization
- Mistake archive — log errors, track knowledge points, review on a spaced-repetition schedule
- Knowledge Garden — shared community Q&A with AI-generated tags, deduplication, and a hierarchical tag tree
- Drawing whiteboard
- AI dashboard with custom AI configuration

**AI Features** (powered by Dify)

- Detect mistakes from chat messages
- Summarize knowledge points from mistakes
- Summarize collaborative documents
- Generate daily learning reports
- Generate next-day chat suggestions
- Analyze mistake patterns (radar chart)
- Generate 5 practice problems from mistake analysis

**Social**

- Add friends
- Shared mistake view between study partners

------

## Tech Stack

| Layer      | Technology                                   |
| ---------- | -------------------------------------------- |
| Backend    | Spring Boot 3, Spring Security, MyBatis-Plus |
| Frontend   | Vue 3, Vite, ECharts                         |
| Database   | MySQL 8                                      |
| Cache      | Redis                                        |
| Real-time  | WebSocket                                    |
| AI         | Dify API                                     |
| Deployment | Alibaba Cloud ECS, Docker                    |

------

## Getting Started

### Prerequisites

- JDK 17+
- Node.js 18+
- MySQL 8
- Redis

### Backend

1. Copy the example config and fill in your values:

```bash
cp backend/src/main/resources/application-example.yml backend/src/main/resources/application.yml
```

1. Edit `application.yml` with your database password, Redis host, JWT secret, and Dify API key.
2. Run:

```bash
cd backend
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

------

## Project Structure

```
Symphony/
├── backend/          # Spring Boot application
│   └── src/
│       └── main/
│           ├── java/
│           └── resources/
│               ├── application-example.yml   # Config template (no secrets)
│               └── application.yml           # Your local config (gitignored)
├── frontend/         # Vue 3 application
└── docs/
    └── screenshots/  # Preview images
```

------

## License

MIT © [Shinomiya-s](https://github.com/Shinomiya-s)