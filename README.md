# Symphony

> An AI-powered collaborative learning platform for students.

Symphony is a full-stack web application that combines real-time collaboration, intelligent mistake tracking, and AI-driven insights to help students learn together more effectively.

------

## Screenshots

### Home

![home](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\home.png)

### Login

![login](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\login.png)

### Study Partner Chat

![chat](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\chat.jpg)

### Mistake Archive

![mistake-archive](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\mistake-archive.png)

### AI Analysis & Practice Problems

![ai-analysis](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\ai-analysis.jpg)

### Knowledge Garden

![knowledge-garden](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\knowledge-garden.png)

### Knowledge Entry Detail

![knowledge-detail](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\knowledge-detail.png)

### Collaborative Document

![collab-doc](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\collab-doc.png)

### Document Archive

![doc-archive](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\doc-archive.png)

### Daily AI Report

![daily-report](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\daily-report.jpg)

### Personal AI Dashboard

![ai-dashboard](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\ai-dashboard.png)

### Drawing Whiteboard

![whiteboard](D:\CS\26年大学生计算机应用能力大赛\Symphony\docs\screenshots\whiteboard.png)

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