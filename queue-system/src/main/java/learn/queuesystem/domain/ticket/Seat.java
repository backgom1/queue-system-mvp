package learn.queuesystem.domain.ticket;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "seats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int seatNo; // 좌석 번호

    @Version // 낙관적 락을 위한 버전
    private Long version;

    private boolean isReserved;

    public Seat(int seatNo) {
        this.seatNo = seatNo;
        this.isReserved = false;
    }

    public void reserve() {
        if (this.isReserved) {
            throw new IllegalStateException("이미 예약된 좌석입니다.");
        }
        this.isReserved = true;
    }
}
