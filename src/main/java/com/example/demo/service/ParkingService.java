package com.example.demo.service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParkingService {

    private final FloorRepository floorRepository;
    private final SlotRepository slotRepository;
    private final ReservationRepository reservationRepository;

    public ParkingService(FloorRepository floorRepository, SlotRepository slotRepository, ReservationRepository reservationRepository) {
        this.floorRepository = floorRepository;
        this.slotRepository = slotRepository;
        this.reservationRepository = reservationRepository;
    }

    public ParkingFloor createFloor(ParkingFloor floor) {
        return floorRepository.save(floor);
    }

    public Slot createSlot(Long floorId, Slot slot) {
        ParkingFloor floor = floorRepository.findById(floorId)
            .orElseThrow(() -> new ResourceNotFoundException("Floor not found with id: " + floorId));
        slot.setParkingFloor(floor);
        return slotRepository.save(slot);
    }

    public Reservation reserveSlot(Reservation reservation) {
        if (!reservation.getStartTime().isBefore(reservation.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }
        if (Duration.between(reservation.getStartTime(), reservation.getEndTime()).toHours() >= 24) {
            throw new IllegalArgumentException("Reservation duration cannot exceed 24 hours.");
        }
        slotRepository.findById(reservation.getSlot().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Slot not found with id: " + reservation.getSlot().getId()));
        List<Reservation> overlaps = reservationRepository.findOverlappingReservations(
            reservation.getSlot().getId(),
            reservation.getStartTime(),
            reservation.getEndTime()
        );
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("Slot is already booked for the selected time range.");
        }
        double durationInMinutes = Duration.between(reservation.getStartTime(), reservation.getEndTime()).toMinutes();
        long roundedHours = (long) Math.ceil(durationInMinutes / 60.0);
        double cost = roundedHours * reservation.getVehicleType().getHourlyRate();
        reservation.setCost(cost);
        return reservationRepository.save(reservation);
    }

    public Reservation getReservationDetails(Long reservationId) {
        return reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));
    }

    public void cancelReservation(Long reservationId) {
        if (!reservationRepository.existsById(reservationId)) {
            throw new ResourceNotFoundException("Reservation not found with id: " + reservationId);
        }
        reservationRepository.deleteById(reservationId);
    }

    public List<Slot> getAvailableSlots(LocalDateTime startTime, LocalDateTime endTime) {
        List<Slot> allSlots = slotRepository.findAll();
        List<Reservation> allOverlappingReservations = allSlots.parallelStream()
            .flatMap(slot -> reservationRepository.findOverlappingReservations(slot.getId(), startTime, endTime).stream())
            .collect(Collectors.toList());
        List<Long> occupiedSlotIds = allOverlappingReservations.stream()
            .map(reservation -> reservation.getSlot().getId())
            .collect(Collectors.toList());
        return allSlots.stream()
            .filter(slot -> !occupiedSlotIds.contains(slot.getId()))
            .collect(Collectors.toList());
    }
}