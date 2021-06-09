# carrent

- 렌트카 등록

http POST http://localhost:8081/products name=Tesla stock=2 productId=1 

http POST http://localhost:8084/bookings qty=1 startDate=2021-07-01 endDate=2021-07-03 status=Booked productId=2
- 렌트카 등록후 상품갯수 수정 
http PATCH http://localhost:8081/products/1 stock=11
- 렌트카 조회
http GET http://localhost:8081/products
- 예약
http POST http://localhost:8084/bookings qty=1 startDate=2021-07-01 endDate=2021-07-03 productId=1
- 예약조회
http GET http://localhost:8084/bookings
- 예약취소
http DELETE http://localhost:8084/bookings/1
- 반납
http PATCH http://localhost:8083/stores/2 status=Returned
- store 조회
http GET http://localhost:8083/stores
- 마이페이지 조회
http GET http://localhost:8082/myPages
