package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.ParkingFloor;
import com.example.demo.model.Reservation;
import com.example.demo.model.Slot;
import com.example.demo.model.VehicleType;
import com.example.demo.repository.FloorRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.SlotRepository;

@ExtendWith(MockitoExtension.class)
class ParkingServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private SlotRepository slotRepository;
    @Mock
    private FloorRepository floorRepository;

    @InjectMocks
    private ParkingService parkingService;

    private Slot availableSlot;
    private Reservation newReservation;
    private ParkingFloor floor;

    @BeforeEach
    void setUp() {
        floor = new ParkingFloor();
        floor.setId(1L);
        floor.setFloorNumber(1);

        availableSlot = new Slot();
        availableSlot.setId(1L);
        availableSlot.setSlotNumber(101);
        availableSlot.setParkingFloor(floor);

        newReservation = new Reservation();
        newReservation.setSlot(availableSlot);
        newReservation.setStartTime(LocalDateTime.now().plusHours(1));
        newReservation.setEndTime(LocalDateTime.now().plusHours(3));
        newReservation.setVehicleNumber("KA01AB1234");
        newReservation.setVehicleType(VehicleType.FOUR_WHEELER);
    }

    @Test
    void whenCreateFloor_thenSucceeds() {
        when(floorRepository.save(any(ParkingFloor.class))).thenReturn(floor);
        ParkingFloor savedFloor = parkingService.createFloor(new ParkingFloor());
        assertNotNull(savedFloor);
    }

    @Test
    void whenCreateSlot_thenSucceeds() {
        when(floorRepository.findById(1L)).thenReturn(Optional.of(floor));
        when(slotRepository.save(any(Slot.class))).thenReturn(availableSlot);
        Slot result = parkingService.createSlot(1L, new Slot());
        assertNotNull(result);
        assertEquals(1L, result.getParkingFloor().getId());
    }

    @Test
    void whenSlotIsAvailable_thenReservationSucceeds() {
        when(slotRepository.findById(1L)).thenReturn(Optional.of(availableSlot));
        when(reservationRepository.findOverlappingReservations(any(), any(), any())).thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class))).thenReturn(newReservation);

        Reservation result = parkingService.reserveSlot(newReservation);

        assertNotNull(result);
        assertEquals(60.0, result.getCost());
    }

    @Test
    void whenSlotIsAlreadyBooked_thenReservationFails() {
        Reservation existingReservation = new Reservation();
        when(slotRepository.findById(1L)).thenReturn(Optional.of(availableSlot));
        when(reservationRepository.findOverlappingReservations(any(), any(), any())).thenReturn(Collections.singletonList(existingReservation));

        Exception exception = assertThrows(IllegalStateException.class, () -> parkingService.reserveSlot(newReservation));
        assertEquals("Slot is already booked for the selected time range.", exception.getMessage());
    }

    @Test
    void whenReservationDurationExceeds24Hours_thenFails() {
        newReservation.setEndTime(newReservation.getStartTime().plusHours(25));
        Exception exception = assertThrows(IllegalArgumentException.class, () -> parkingService.reserveSlot(newReservation));
        assertEquals("Reservation duration cannot exceed 24 hours.", exception.getMessage());
    }
    
    @Test
    void whenCancelReservation_thenSucceeds() {
        when(reservationRepository.existsById(1L)).thenReturn(true);
        doNothing().when(reservationRepository).deleteById(1L);
        assertDoesNotThrow(() -> parkingService.cancelReservation(1L));
        verify(reservationRepository, times(1)).deleteById(1L);
    }

    @Test
    void whenCancelNonExistentReservation_thenFails() {
        when(reservationRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> parkingService.cancelReservation(99L));
    }
}