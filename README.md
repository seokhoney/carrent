![image](https://user-images.githubusercontent.com/84000863/121391703-a45a0e80-c989-11eb-83bb-dbf8f8202686.png)

# 렌터카

본 프로젝트는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성한 프로젝트입니다.
이는 클라우드 네이티브 애플리케이션의 개발에 요구되는 체크포인트들을 통과하기 위한 내용을 포함합니다.

# 서비스 시나리오

기능적 요구사항
1. 고객이 차량 및 날짜를 선택하여 예약한다.
2. 예약이 되면 예약 내역이 렌터카 업체에게 전달된다.
3. 예약 내역이 렌터카 업체에 전달되는 동시에, 대여 가능 수량이 변경된다.
4. 업체에서 예약 내역을 확인하여 차량을 준비한다.
5. 차량을 준비 후, 렌트된 상태로 변경된다.
6. 고객이 예약을 취소할 수 있다.
7. 고객이 차량을 반납하면 대여 가능한 수량이 증가된다.
8. 고객이 예약정보를 중간중간 조회한다.
   ex) 예약시작날짜, 예약종료날짜, 수량 등
9. 업체는 새로운 차량을 등록할 수 있다.

비기능적 요구사항
1. 트랜잭션
    1. 예약된 주문건은 대여 가능 수량이 변경되어야 한다. Sync 호출
2. 장애격리
    1. 업체관리 기능이 수행되지 않더라도 예약은 365일 24시간 받을 수 있어야 한다.  Async (event-driven), Eventual Consistency
    2. 예약시스템이 과중되면 사용자를 잠시동안 받지 않고 예약을 잠시후에 하도록 유도한다.  Circuit breaker
3. 성능
    1. 고객이 예약상태를 별도의 고객페이지에서 확인할 수 있어야 한다. CQRS

# 체크포인트

1. Saga
2. CQRS
3. Correlation
4. Req/Resp
5. Gateway
6. Deploy/ Pipeline
7. Circuit Breaker
8. Autoscale (HPA)
9. Zero-downtime deploy
10. Config Map / Persistence Volume
11. Polyglot
12. Self-healing (Liveness Probe)

# 분석/설계

## AS-IS 조직 (Horizontally-Aligned)
  ![image](https://user-images.githubusercontent.com/487999/79684144-2a893200-826a-11ea-9a01-79927d3a0107.png)

## TO-BE 조직 (Vertically-Aligned)
  ![image](https://user-images.githubusercontent.com/84000863/121344166-65fb2a00-c95e-11eb-97b3-8d1490beb909.png)


## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/tdKjnnj8k4dt4Pik8DOnp0yYffp2/share/7d3366945eb432ceb06191adb4fca105


### 이벤트 도출
![image](https://user-images.githubusercontent.com/84000863/121367196-11af7480-c975-11eb-9cc9-2b3a693f1ec5.png)

### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/84000863/121383633-8dfc8480-c982-11eb-8e05-a111676932d6.png)

    - 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함
        - 예약내역 조회됨 :  UI 의 이벤트이지, 업무적인 의미의 이벤트가 아니라서 제외

### 액터, 커맨드 부착하여 읽기 좋게
![image](https://user-images.githubusercontent.com/84000863/121384293-1713bb80-c983-11eb-8cf1-5b30cbfe41ee.png)

### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/84000863/121385028-bafd6700-c983-11eb-9b9d-26ccaaf47ec5.png)

    - product, book의 상품과 예약, store의 업체는 그와 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들 끼리 묶어줌

### 바운디드 컨텍스트로 묶기

![image](https://user-images.githubusercontent.com/84000863/121385407-0b74c480-c984-11eb-9f4a-53137a4601af.png)

    - 도메인 서열 분리 
        - Core Domain:  product, book, store : 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 배포주기는 app, book, store 의 경우 1주일 1회 미만
        - Supporting Domain:  -- : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함.

### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)

![image](https://user-images.githubusercontent.com/84000863/121385551-2cd5b080-c984-11eb-8430-77f6e06d9581.png)

### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)

![image](https://user-images.githubusercontent.com/84000863/121385879-78885a00-c984-11eb-9309-c82ac0c9d9b0.png)

### 완성된 1차 모형

![image](https://user-images.githubusercontent.com/84000863/121388618-96ef5500-c986-11eb-9bde-63d46690caeb.png)

    - View Model 추가

### 1차 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증

![image](https://user-images.githubusercontent.com/84000863/121388710-ab335200-c986-11eb-966a-3f30b94ba7c0.png)

    - 고객이 차량을 예약한다 (ok)
    - 예약이 되면 렌터카 업체에게 전달된다 (ok)
    - 예약 내역이 렌터카 업체에 전달되는 동시에, 대여 가능 수량이 변경된다. (ok)
    - 업체에서 예약 내역을 확인하여 차량을 준비한다. (ok)
    - 차량을 준비 후, 렌트되면 렌트된 상태로 변경된다. (ok)

![image](https://user-images.githubusercontent.com/84000863/121389124-0cf3bc00-c987-11eb-9c44-c437bda469c6.png)

    - 고객이 주문을 취소할 수 있다 (ok)
    - 고객이 차량을 반납하면 대여 가능 수량이 변경된다. (ok)
    - 고객이 예약정보를 중간중간 조회한다. (View-green sticker 의 추가로 ok) 


### 비기능 요구사항에 대한 검증

![image](https://user-images.githubusercontent.com/84000863/121389596-8db2b800-c987-11eb-9ba7-d03b9f7b04aa.png)

    - 1) 예약된 주문건은 대여 가능 수량이 변경되어야 한다. (Req/Res)
    - 2) 업체관리 기능이 수행되지 않더라도 예약은 365일 24시간 받을 수 있어야 한다. (Pub/sub)
    - 3) 차량등록 시스템이 과중되면 잠시동안 받지 않고 등록을 잠시후에 하도록 유도한다 (Circuit breaker)
    - 4) 고객이 예약상태를 별도의 고객페이지에서 확인할 수 있어야 한다 (CQRS)


## 헥사고날 아키텍처 다이어그램 도출
    
![image](https://user-images.githubusercontent.com/84000863/122320373-15d32780-cf5d-11eb-95b7-23935cda9bb4.png)

    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현:

구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd product
mvn spring-boot:run

cd booking
mvn spring-boot:run 

cd store
mvn spring-boot:run  

cd customercenter
mvn spring-boot:run

```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다. 아래 Product가 그 예시이다.

```
package carrent;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Product_table")
public class Product {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String name;
    private Integer stock;
    private Long productId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

}

```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package carrent;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="products", path="products")
public interface ProductRepository extends PagingAndSortingRepository<Product, Long>{
    Product findByProductId(Long productId);

}
```
- 적용 후 REST API 의 테스트
```
# booking 서비스의 예약처리
http POST http://localhost:8084/bookings qty=1 startDate=2021-07-01 endDate=2021-07-03 productId=1

# 주문 상태 확인
http GET http://localhost:8084/bookings

```


## 폴리글랏 퍼시스턴스

product 서비스와 booking 서비스는 h2 DB로 구현하고, 그와 달리 store 서비스의 경우 Hsql DB로 구현하여, MSA간 서로 다른 종류의 DB간에도 문제 없이 동작하여 다형성을 만족하는지 확인하였다.

- product, booking 서비스의 pom.xml 설정

![image](https://user-images.githubusercontent.com/84000863/122320251-ed4b2d80-cf5c-11eb-85a9-e3a43e3e56d2.png)

- store 서비스의 pom.xml 설정

![image](https://user-images.githubusercontent.com/84000863/122320209-ddcbe480-cf5c-11eb-920c-4d3f86cac072.png)


## CQRS

Viewer를 별도로 구현하여 아래와 같이 view가 출력된다.

- 예약 수행 후의 myPage

![image](https://user-images.githubusercontent.com/84000863/122181119-0784fd00-cec4-11eb-9a49-2881e440e5a1.png)

- 반납 수행 후의 myPage

![image](https://user-images.githubusercontent.com/84000863/122181167-153a8280-cec4-11eb-9a11-b81e993cf9f6.png)


## 동기식 호출

분석단계에서의 조건 중 하나로 예약(booking)->업체(pay) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 결제서비스를 호출하기 위하여 FeignClient를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
# (booking) ProductService.java


package carrent.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;



@FeignClient(name="product", url="http://product:8080") 
public interface ProductService {

    @RequestMapping(method= RequestMethod.GET, path="/chkAndModifyStock")
    public boolean modifyStock(@RequestParam("productId") Long productId,
                            @RequestParam("qty") Integer qty);

}
```

- 예약된 직후(@PostPersist) 재고수량이 업데이트 되도록 처리 (modifyStock 호출)
```
# Order.java (Entity)

    @PostPersist
    public void onPostPersist() {

            boolean rslt = BookingApplication.applicationContext.getBean(carrent.external.ProductService.class)
            .modifyStock(this.getProductId(), this.getQty());

            if (rslt) {
                
                Booked booked = new Booked();
                booked.setStatus("Booked");
                BeanUtils.copyProperties(this, booked);
                booked.publishAfterCommit();
            } 
    }
    
```

- 재고수량은 아래와 같은 로직으로 처리
```
public boolean modifyStock(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
                boolean status = false;
                Long productId = Long.valueOf(request.getParameter("productId"));
                int qty = Integer.parseInt(request.getParameter("qty"));

                Product product = productRepository.findByProductId(productId);

                if(product != null){
                        if (product.getStock() >= qty) {
                                product.setStock(product.getStock() - qty);
                                productRepository.save(product);
                                status = true;
                        }
                }

                return status;
        }
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 상품 시스템이 장애가 나면 예약도 못하는 것을 확인:



- 상품(product) 서비스를 잠시 내려놓음 (ctrl+c)

- 예약하기(booking)
```
http POST http://localhost:8084/bookings qty=1 startDate=2021-07-01 endDate=2021-07-03 productId=1 
```
< Fail >

![image](https://user-images.githubusercontent.com/84000863/122181816-a9a4e500-cec4-11eb-980a-db584dc11d61.png)

- 상품(product) 서비스 재기동
```
cd product
mvn spring-boot:run
```

- 예약하기(booking)
```
http POST http://localhost:8084/bookings qty=1 startDate=2021-07-01 endDate=2021-07-03 productId=1 
```
< Success >

![image](https://user-images.githubusercontent.com/84000863/122181996-d1944880-cec4-11eb-9ddb-be0ec470ddc2.png)



## Gateway 적용

- gateway > applitcation.yml 설정

```
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: product
          uri: http://localhost:8081
          predicates:
            - Path=/products/**, /chkAndModifyStock/** 
        - id: customercenter
          uri: http://localhost:8082
          predicates:
            - Path= /myPages/**
        - id: store
          uri: http://localhost:8083
          predicates:
            - Path=/stores/** 
        - id: booking
          uri: http://localhost:8084
          predicates:
            - Path=/bookings/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: product
          uri: http://product:8080
          predicates:
            - Path=/products/** , /chkAndModifyStock/** 
        - id: customercenter
          uri: http://customercenter:8080
          predicates:
            - Path= /myPages/**
        - id: store
          uri: http://store:8080
          predicates:
            - Path=/stores/** 
        - id: booking
          uri: http://booking:8080
          predicates:
            - Path=/bookings/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
  
```

- gateway 테스트

```
http http://localhost:8088/product
```
![image](https://user-images.githubusercontent.com/84000863/122182444-423b6500-cec5-11eb-932d-77e066f60f94.png)


## 비동기식 호출 / 시간적 디커플링 / 장애격리 (수정 필요)

예약(booking)이 이루어진 후에 업체(store)에서 차를 배차하는 행위는 동기식이 아니라 비 동기식으로 처리하여 업체(store)의 배차처리를 위하여 예약이 블로킹 되지 않도록 처리한다.

- 이를 위하여 예약완료 되었음을 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
    @PostPersist
    public void onPostPersist() {


            boolean rslt = BookingApplication.applicationContext.getBean(carrent.external.ProductService.class)
            .modifyStock(this.getProductId(), this.getQty());

            if (rslt) {
                
                Booked booked = new Booked();
                booked.setStatus("Booked");
                BeanUtils.copyProperties(this, booked);
                booked.publishAfterCommit();
            } 
    }
```

- 업체(store)에서는 예악완료(Booked) 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
package carrent;

...

@Service
public class PolicyHandler{
    @Autowired 
    StoreRepository storeRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBooked_PrepareCar(@Payload Booked booked){
        /*
        if(booked.isMe()){        
            Optional<Store> optionalStore= storeRepository.findById(booked.getId());
            Store store = optionalStore.get();
            storeRepository.save(store);
          }
          */
          if(booked.isMe()){            
            Store store = new Store();
            store.setBookingId(booked.getId());
            store.setProductId(booked.getProductId());        
            store.setStatus("CarRentStarted");
            store.setQty(booked.getQty());
            storeRepository.save(store);
        }  
            
    }
    
...

```
(수정 필요)
booking 서비스는 store 서비스와 완전히 분리되어있으며(sync transaction 없음) 이벤트 수신에 따라 처리되기 때문에, store 서비스가 유지보수로 인해 잠시 내려간 상태라도 예약을 진행해도 문제 없다.(시간적 디커플링) :
  
```
# 업체(store) 서비스 를 잠시 내려놓음 (ctrl+c)

# 예약처리
http POST http://localhost:8084/bookings qty=1 startDate=2021-07-01 endDate=2021-07-03 productId=1

# 예약 상태 확인
http GET http://localhost:8084/bookings     # 예약상태 안바뀜 확인
```
<...image>

```
# 업체(store) 서비스 기동
cd store
mvn spring-boot:run

# 예약 상태 확인
http GET http://localhost:8084/bookings     # 예약 상태가 "CarRentStarted"으로 확인
```
<...image>


## Deploy / Pipeline

- 소스 가져오기
```
git clone https://github.com/kary000/car.git
```
![image](https://user-images.githubusercontent.com/84000863/122197088-cea05480-ced2-11eb-8a64-9c2d55b41240.png)

- 빌드하기
```
cd booking
mvn package

cd customercenter
mvn package

cd gateway
mvn package

cd product
mvn package

cd store
mvn package
```

![image](https://user-images.githubusercontent.com/84000863/122197418-1de68500-ced3-11eb-8b10-7820e8a354b8.png)

- 도커라이징(Dockerizing) : Azure Container Registry(ACR)에 Docker Image Push하기
```
cd booking
az acr build --registry user08skccacr --image user08skccacr.azurecr.io/booking:latest .

cd customercenter
az acr build --registry user08skccacr --image user08skccacr.azurecr.io/customercenter:latest .

cd gateway
az acr build --registry user08skccacr --image user08skccacr.azurecr.io/gateway:latest .

cd product
az acr build --registry user08skccacr --image user08skccacr.azurecr.io/product:latest .

cd store
az acr build --registry user08skccacr --image user08skccacr.azurecr.io/store:latest . 

```
![image](https://user-images.githubusercontent.com/84000863/122322876-22597f00-cf61-11eb-90ff-bb7b26b1c21f.png)

- 컨테이너라이징(Containerizing) : Deployment 생성 확인
```
kubectl create deploy booking --image=user08skccacr.azurecr.io/booking:v1
kubectl create deploy customercenter --image=user08skccacr.azurecr.io/customercenter:v1
kubectl create deploy gateway --image=user08skccacr.azurecr.io/gateway:v1
kubectl create deploy product --image=user08skccacr.azurecr.io/product:v1
kubectl create deploy store --image=user08skccacr.azurecr.io/store:v1

kubectl get all
```
![image](https://user-images.githubusercontent.com/84000863/122198125-d6142d80-ced3-11eb-8d04-be7fd397a1db.png)

- 컨테이너라이징(Containerizing) : Service 생성 확인
```
kubectl expose deploy booking --type="ClusterIP" --port=8080
kubectl expose deploy customercenter --type="ClusterIP" --port=8080
kubectl expose deploy gateway --type="LoadBalancer" --port=8080
kubectl expose deploy product --type="ClusterIP" --port=8080
kubectl expose deploy store --type="ClusterIP" --port=8080

kubectl get all
```
![image](https://user-images.githubusercontent.com/84000863/122198270-f93edd00-ced3-11eb-8589-42010860180b.png)

- (+) deployment.yaml로 배포
```
cd booking
kubectl apply -f kubernetes/deployment.yml

cd customercenter
kubectl apply -f kubernetes/deployment.yml

cd product
kubectl apply -f kubernetes/deployment.yml

cd store
kubectl apply -f kubernetes/deployment.yml

cd gateway
kubectl create deploy gateway --image=user08skccacr.azurecr.io/gateway:latest
```
![image](https://user-images.githubusercontent.com/84000863/122198598-428f2c80-ced4-11eb-8a98-f93466901302.png)


## 동기식 호출 / 서킷 브레이킹 / 장애격리 (수정 필요)

* 서킷 브레이킹 : yaml파일에서 구현함

시나리오는 차량등록시 요청이 과도할 경우 CB 를 통하여 장애격리.

outlierDetection 를 설정: 1초내에 연속 한번 오류발생시, 호스트를 10초동안 100% CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정

```
# dr-htttpbin.yaml

outlierDetection:
        consecutive5xxErrors: 1
        interval: 1s
        baseEjectionTime: 10s
        maxEjectionPercent: 100

```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 100명
- 30초 동안 실시

```
$ siege -c100 -t30S -r10 --content-type "application/json" 'http://localhost:8081/orders POST {"item": "chicken"}'

** SIEGE 4.0.5
** Preparing 100 concurrent users for battle.
The server is now under siege...

HTTP/1.1 201     0.68 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.68 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.70 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.70 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.73 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.75 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.77 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.97 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.81 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.87 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.12 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.16 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.17 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.26 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.25 secs:     207 bytes ==> POST http://localhost:8081/orders

* 요청이 과도하여 CB를 동작함 요청을 차단

HTTP/1.1 500     1.29 secs:     248 bytes ==> POST http://localhost:8081/orders   
HTTP/1.1 500     1.24 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     1.23 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     1.42 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     2.08 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.29 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     1.24 secs:     248 bytes ==> POST http://localhost:8081/orders

* 요청을 어느정도 돌려보내고나니, 기존에 밀린 일들이 처리되었고, 회로를 닫아 요청을 다시 받기 시작

HTTP/1.1 201     1.46 secs:     207 bytes ==> POST http://localhost:8081/orders  
HTTP/1.1 201     1.33 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.36 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.63 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.65 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.68 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.69 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.71 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.71 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.74 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.76 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.79 secs:     207 bytes ==> POST http://localhost:8081/orders

* 다시 요청이 쌓이기 시작하여 건당 처리시간이 610 밀리를 살짝 넘기기 시작 => 회로 열기 => 요청 실패처리

HTTP/1.1 500     1.93 secs:     248 bytes ==> POST http://localhost:8081/orders    
HTTP/1.1 500     1.92 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     1.93 secs:     248 bytes ==> POST http://localhost:8081/orders

* 생각보다 빨리 상태 호전됨 - (건당 (쓰레드당) 처리시간이 610 밀리 미만으로 회복) => 요청 수락

HTTP/1.1 201     2.24 secs:     207 bytes ==> POST http://localhost:8081/orders  
HTTP/1.1 201     2.32 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.16 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.19 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.19 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.19 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.21 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.29 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.30 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.38 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.59 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.61 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.62 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.64 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.01 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.27 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.33 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.45 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.52 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.57 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.69 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.70 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.69 secs:     207 bytes ==> POST http://localhost:8081/orders

* 이후 이러한 패턴이 계속 반복되면서 시스템은 도미노 현상이나 자원 소모의 폭주 없이 잘 운영됨


HTTP/1.1 500     4.76 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     4.23 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.76 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.74 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     4.82 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.82 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.84 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.66 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     5.03 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     4.22 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     4.19 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     4.18 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.69 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.65 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     5.13 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     4.84 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     4.25 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     4.25 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.80 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     4.87 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     4.33 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.86 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     4.96 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     4.34 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     4.04 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.50 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.95 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.54 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.65 secs:     207 bytes ==> POST http://localhost:8081/orders


:
:

Transactions:		        1025 hits
Availability:		       63.55 %
Elapsed time:		       59.78 secs
Data transferred:	        0.34 MB
Response time:		        5.60 secs
Transaction rate:	       17.15 trans/sec
Throughput:		        0.01 MB/sec
Concurrency:		       96.02
Successful transactions:        1025
Failed transactions:	         588
Longest transaction:	        9.20
Shortest transaction:	        0.00

```
- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌. 하지만, 63.55% 가 성공하였고, 46%가 실패했다는 것은 고객 사용성에 있어 좋지 않기 때문에 Retry 설정과 동적 Scale out (replica의 자동적 추가,HPA) 을 통하여 시스템을 확장 해주는 후속처리가 필요.

- Retry 의 설정 (istio)
- Availability 가 높아진 것을 확인 (siege)

### Autoscale (HPA)
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 


- 상품(product) 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다:
```
kubectl autoscale deploy product --min=1 --max=10 --cpu-percent=5

kubectl get hpa
```
![image](https://user-images.githubusercontent.com/84000863/122199493-0b6d4b00-ced5-11eb-8c52-71b7d75a49ab.png)

- CB 에서 했던 방식대로 워크로드를 30초 동안 걸어준다.
```
siege -c100 -t30S -v --content-type "application/json" 'http://product:8080/products POST {"productId":"123", "stock":"5", "name":"IONIQ"}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:
```
kubectl get deploy product -w
watch -n 1 kubectl get pod
```

- 어느정도 시간이 흐른 후 (약 30초) 스케일 아웃이 벌어지는 것을 확인할 수 있다:

![image](https://user-images.githubusercontent.com/84000863/122199928-83d40c00-ced5-11eb-9849-87b3e14297f1.png)


- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다. 

![image](https://user-images.githubusercontent.com/84000863/122199996-96e6dc00-ced5-11eb-87c7-c3316bdc73a0.png)


## 무정지 재배포

* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함

- seige 로 배포작업 직전에 워크로드를 모니터링 함.
```
siege -c250 -t60S  -v --content-type "application/json" 'http://52.231.159.123:8080/products POST {"productId":”1”, "name":Tesla”, “stock”=”2”}'
```
- Readiness가 설정되지 않은 yml 파일로 배포 중 서비스 요청 처리 실패

![image](https://user-images.githubusercontent.com/84000863/122200277-d7def080-ced5-11eb-951b-33f18e4c2d58.png)

- deployment.yml에 readiness 옵션을 추가

![image](https://user-images.githubusercontent.com/84000863/122200434-0066ea80-ced6-11eb-82a8-e94ed0182a51.png)

- Readiness가 설정된 yml 파일로 배포 진행
```
kubectl apply -f deployment_with_readiness.yml
```

- 기존 버전과 새 버전의 store pod 공존 중

![image](https://user-images.githubusercontent.com/84000863/122200647-32784c80-ced6-11eb-81d8-347e75c35f3a.png)

 - Availability가 배포기간 동안 변화가 없기 때문에 무정지 재배포가 성공한 것으로 확인됨.

![image](https://user-images.githubusercontent.com/84000863/122200849-62275480-ced6-11eb-9350-f6af2e3296fb.png)


## ConfigMap (수정 필요)

- Store 서비스의 deployment.yml 파일에 아래 항목 추가
```
env:
   - name: STATUS
     valueFrom:
       configMapKeyRef:
         name: store-cm
         key: status
```

- ConfigMap 생성 및 조회
```
kubectl create configmap storecm --from-literal=status=Preparing
kubectl get configmap storecm -o yaml
```

![image](https://user-images.githubusercontent.com/84000863/122202883-6f454300-ced8-11eb-8952-ba5f1c463ce6.png)

- ...

## Self-Healing (Liveness Probe)

- 상품(product) 서비스의 deployment.yaml에 liveness probe 옵션 추가
```
livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8090
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
 ```
 
- product에 liveness 적용 확인

![image](https://user-images.githubusercontent.com/84000863/122203276-d4993400-ced8-11eb-98d3-b3044f9eb667.png)

- product 서비스에 liveness가 발동되었고, 포트에 응답이 없기에 Restart가 발생함

![image](https://user-images.githubusercontent.com/84000863/122203358-ebd82180-ced8-11eb-968a-2ecf20354f1d.png)
