package com.example.demo.repository;

import com.example.demo.model.ParkingFloor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<ParkingFloor, Long> {
}