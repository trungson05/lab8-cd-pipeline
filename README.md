# Lab 8 — Jenkins CI/CD

Lab 8 dùng Java 17 và Maven để chạy các bước CI của Lab 7 trên chính ứng dụng HTTP sẽ được triển khai. Jenkins ghi nhận kết quả JUnit, gửi mã nguồn tới SonarQube, đóng gói JAR và xuất bản vào kho `maven-snapshots` trên Nexus. Docker dùng cùng tệp JAR đó để tạo image `trungson05/devops-lab8-app`.

## Chạy trên Jenkins của Lab 7

Trong job Lab 8, chọn **Pipeline script from SCM**, trỏ tới repository Lab 8 và đặt **Script Path** là `Jenkinsfile` tại gốc repository. Agent cần JDK 17 trở lên, Maven có tên `M3`, Docker CLI và quyền truy cập Docker daemon như ở Lab 8.

| Cấu hình Jenkins | Giá trị pipeline sử dụng |
| --- | --- |
| SonarQube installation | `SonarQube`, credential `sonarqube-token` có quyền phân tích project `devops-lab8` |
| Nexus | Credential `nexus-credentials`, managed file `maven-settings-nexus` với server ID `nexus` |
| Docker Hub | Credential `docker-hub-credentials_1` có quyền push vào `trungson05/devops-lab8-app` |

Khi chạy **Build Now**, Jenkins chỉ bắt đầu CD sau khi CI thành công. Pipeline chạy Docker Scout nếu khả dụng, đẩy hai tag lên Docker Hub (số build và `latest`), rồi triển khai bằng tag số build. Trên Docker Desktop, có thể kiểm tra kết quả ở [trang ứng dụng](http://localhost:3002/) và [endpoint kiểm tra trạng thái](http://localhost:3002/health).

## Kiểm thử cục bộ

Chạy kiểm thử và khởi động ứng dụng từ thư mục gốc của Lab 8.

```sh
mvn -f app/pom.xml clean verify
java -jar app/target/devops-lab8-app.jar
```

Ứng dụng mặc định nghe ở cổng `3000`. Lệnh `docker compose up --build` cũng có thể dựng ứng dụng trực tiếp từ mã nguồn cùng với Nginx và Redis.
