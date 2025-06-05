package com.example.attendancesystem.repository;

import com.example.attendancesystem.model.NfcCard;
import com.example.attendancesystem.model.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NfcCardRepository extends JpaRepository<NfcCard, Long> {
    Optional<NfcCard> findByCardUid(String cardUid);
    Optional<NfcCard> findBySubscriber(Subscriber subscriber);
    boolean existsByCardUid(String cardUid);
}
