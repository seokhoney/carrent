# carrent

- 렌트카1 등록
http POST http://localhost:8081/products name=Tesla stock=2 productId=1
- 렌트카2 등록
http POST http://localhost:8081/products name=IONIQ stock=4 productId=2 
- 렌트카1 상품갯수 수정 
http PATCH http://localhost:8081/products/1 stock=11
- 렌트카 조회
http GET http://localhost:8081/products

- 예약1
http POST http://localhost:8084/bookings qty=1 startDate=2021-07-01 endDate=2021-07-03 productId=1
- 예약2
http POST http://localhost:8084/bookings qty=1 startDate=2021-07-01 endDate=2021-07-03 productId=2
- 예약조회
http GET http://localhost:8084/bookings
- 예약1 취소
http DELETE http://localhost:8084/bookings/1

- 렌트카2 반납
http PATCH http://localhost:8083/stores/2 status=Returned

- store 조회
http GET http://localhost:8083/stores
- 마이페이지 조회
http GET http://localhost:8082/myPages
