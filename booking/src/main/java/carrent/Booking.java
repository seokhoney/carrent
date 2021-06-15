package carrent;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Booking_table")
public class Booking {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Integer qty;
    private String status;
    private Date startDate;
    private Date endDate;
    private Long productId;

    @PostPersist
    public void onPostPersist(){

        //try{
            boolean rslt = BookingApplication.applicationContext.getBean(carrent.external.ProductService.class)
            .modifyStock(this.getProductId(), this.getQty());
            System.out.println(rslt);

            if (rslt) {
                Booked booked = new Booked();
                booked.setStatus("Booked");
                BeanUtils.copyProperties(this, booked);
                booked.publishAfterCommit();
            } else {throw new BookingException("No Available stock!");}
            
        //}catch (Exception e) {
       //     e.printStackTrace();
       //}

    }

    @PreRemove
    public void onPreRemove(){
        carrent.external.Cancellation cancellation = new carrent.external.Cancellation();
        // mappings goes here
        cancellation.setBookingId(this.getId());

        this.setStatus("Cancelled");
        BookingCancelled bookingCancelled = new BookingCancelled();
        BeanUtils.copyProperties(this, bookingCancelled);
        bookingCancelled.publishAfterCommit();
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }




}
