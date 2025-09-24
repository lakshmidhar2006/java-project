package com.example.demo.repository;

import com.example.demo.model.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByParkingFloorId(Long floorId);
}