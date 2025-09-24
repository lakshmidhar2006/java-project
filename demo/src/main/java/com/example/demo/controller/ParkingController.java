package com.example.demo.controller; 

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity; 
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ReservationRequestDTO;
import com.example.demo.model.ParkingFloor;
import com.example.demo.model.Reservation;
import com.example.demo.model.Slot;
import com.example.demo.repository.SlotRepository;
import com.example.demo.service.ParkingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api") 
public class ParkingController {

    private final ParkingService parkingService;
    private final SlotRepository slotRepository; 

    public ParkingController(ParkingService parkingService, SlotRepository slotRepository) {
        this.parkingService = parkingService;
        this.slotRepository = slotRepository;
    }

    // 3.1. POST /floors – Create a parking floor
    @PostMapping("/floors")
    public ResponseEntity<ParkingFloor> createFloor(@RequestBody ParkingFloor floor) {
        return ResponseEntity.ok(parkingService.createFloor(floor));
    }

    // 3.2. POST /slots – Create parking slots for a floor
    @PostMapping("/slots")
    public ResponseEntity<Slot> createSlot(@RequestParam Long floorId, @RequestBody Slot slot) {
        return ResponseEntity.ok(parkingService.createSlot(floorId, slot));
    }

    // 3.4. GET /availability – List available slots for a given time range
    @GetMapping("/availability")
    public ResponseEntity<List<Slot>> getAvailableSlots(
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
        return ResponseEntity.ok(parkingService.getAvailableSlots(startTime, endTime));
    }

    // 3.3. POST /reserve – Reserve a slot for a given time range
    @PostMapping("/reserve")
    public ResponseEntity<Reservation> reserveSlot(@Valid @RequestBody ReservationRequestDTO requestDTO) {
        // Find the actual Slot entity from the database using the ID from the request
        Slot slot = slotRepository.findById(requestDTO.getSlotId())
                .orElseThrow(() -> new RuntimeException("Slot not found with id: " + requestDTO.getSlotId()));

        // Convert the DTO to a Reservation entity
        Reservation reservation = new Reservation();
        reservation.setSlot(slot);
        reservation.setStartTime(requestDTO.getStartTime());
        reservation.setEndTime(requestDTO.getEndTime());
        reservation.setVehicleNumber(requestDTO.getVehicleNumber());
        reservation.setVehicleType(requestDTO.getVehicleType());

        // Call the service to perform the reservation logic
        Reservation createdReservation = parkingService.reserveSlot(reservation);
        return ResponseEntity.ok(createdReservation);
    }

    // 3.5. GET /reservations/{id} – Fetch reservation details
    @GetMapping("/reservations/{id}")
    public ResponseEntity<Reservation> getReservationDetails(@PathVariable Long id) {
        return ResponseEntity.ok(parkingService.getReservationDetails(id));
    }

    // 3.6. DELETE /reservations/{id} – Cancel a reservation
    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        parkingService.cancelReservation(id);
        return ResponseEntity.noContent().build(); // Return 204 No Content on successful deletion
    }
}