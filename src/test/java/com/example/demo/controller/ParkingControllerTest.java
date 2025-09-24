package com.example.demo.controller;

import com.example.demo.model.ParkingFloor;
import com.example.demo.model.Reservation;
import com.example.demo.model.Slot;
import com.example.demo.model.VehicleType;
import com.example.demo.repository.SlotRepository;
import com.example.demo.service.ParkingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingController.class)
class ParkingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParkingService parkingService;

    @MockBean
    private SlotRepository slotRepository; // This is also needed by the controller

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }


    @Test
    void whenCreateFloor_thenReturns200() throws Exception {
        ParkingFloor floor = new ParkingFloor();
        floor.setId(1L);
        floor.setFloorNumber(1);

        when(parkingService.createFloor(any(ParkingFloor.class))).thenReturn(floor);

        mockMvc.perform(post("/api/floors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParkingFloor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.floorNumber").value(1));
    }

    @Test
    void whenGetReservationDetails_withValidId_thenReturns200() throws Exception {
        Slot testSlot = new Slot();
        testSlot.setId(1L);
        Reservation sampleReservation = new Reservation();
        sampleReservation.setId(1L);
        sampleReservation.setSlot(testSlot);
        sampleReservation.setVehicleNumber("TS09AB1234");
        sampleReservation.setVehicleType(VehicleType.TWO_WHEELER);
        sampleReservation.setStartTime(LocalDateTime.of(2025, 9, 25, 10, 0));
        sampleReservation.setEndTime(LocalDateTime.of(2025, 9, 25, 11, 0));
        sampleReservation.setCost(20.0);

        when(parkingService.getReservationDetails(1L)).thenReturn(sampleReservation);
        
        mockMvc.perform(get("/api/reservations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.vehicleNumber").value("TS09AB1234"));
    }

    @Test
    void whenReserveSlot_withInvalidVehicleNumber_thenReturns400() throws Exception {
        String invalidReservationJson = "{\"slotId\":1,\"startTime\":\"2025-10-20T10:00:00\",\"endTime\":\"2025-10-20T12:00:00\",\"vehicleNumber\":\"INVALID-123\",\"vehicleType\":\"FOUR_WHEELER\"}";

        mockMvc.perform(post("/api/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidReservationJson))
                .andExpect(status().isBadRequest()); // Expect Bad Request due to validation failure
    }

    @Test
    void whenCancelReservation_thenReturns204() throws Exception {
        doNothing().when(parkingService).cancelReservation(1L);

        mockMvc.perform(delete("/api/reservations/1"))
                .andExpect(status().isNoContent()); // Expect 204 No Content
    }
}