package com.driver.services;


import com.driver.EntryDto.BookTicketEntryDto;
import com.driver.model.Passenger;
import com.driver.model.Ticket;
import com.driver.model.Train;
import com.driver.repository.PassengerRepository;
import com.driver.repository.TicketRepository;
import com.driver.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TicketService {

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    TrainRepository trainRepository;

    @Autowired
    PassengerRepository passengerRepository;


    public Integer bookTicket(BookTicketEntryDto bookTicketEntryDto)throws Exception{

        //Check for validity
        Train trains=trainRepository.findById(bookTicketEntryDto.getTrainId()).get();

        //Use bookedTickets List from the TrainRepository to get bookings done against that trains
        int bookedTickets=0;
        List<Ticket>seats=trains.getBookedTickets();
        for(Ticket ticket:seats){
            bookedTickets+=ticket.getPassengersList().size();
        }


        // Incase the there are insufficient tickets
        // throw new Exception("Less tickets are available");
        if(bookedTickets+bookTicketEntryDto.getNoOfSeats()> trains.getNoOfSeats()){
            throw new Exception("Less tickets are available");
        }

        //otherwise book the ticket, calculate the price and other details
        //Save the information in corresponding DB Tables
        String platform[]=trains.getRoute().split(",");
        List<Passenger>passengerList=new ArrayList<>();
        List<Integer>ids=bookTicketEntryDto.getPassengerIds();
        for(int index: ids){
            passengerList.add(passengerRepository.findById(index).get());
        }
        int start=-1,end=-1;
        for(int i=0;i<platform.length;i++){
            if(bookTicketEntryDto.getFromStation().toString().equals(platform[i])){
                start=i;
                break;
            }
        }
        for(int i=0;i<platform.length;i++){
            if(bookTicketEntryDto.getToStation().toString().equals(platform[i])){
                end=i;
                break;
            }
        }

        Ticket tck=new Ticket();
        tck.setPassengersList(passengerList);
        tck.setFromStation(bookTicketEntryDto.getFromStation());
        tck.setToStation(bookTicketEntryDto.getToStation());


        //Fare System : Check problem statement
        //Incase the trains doesn't pass through the requested platform
        //throw new Exception("Invalid platform");
        if(start==-1||end==-1||end-start<0){
            throw new Exception("Invalid platform");
        }


        //Save the bookedTickets in the trains Object
        int fairSystem=0;
        fairSystem=bookTicketEntryDto.getNoOfSeats()*(end-start)*300;

        tck.setTotalFare(fairSystem);
        tck.setTrain(trains);

        trains.getBookedTickets().add(tck);
        trains.setNoOfSeats(trains.getNoOfSeats()-bookTicketEntryDto.getNoOfSeats());

        //Also in the passenger Entity change the attribute bookedTickets by using the attribute bookingPersonId.
        Passenger passenger=passengerRepository.findById(bookTicketEntryDto.getBookingPersonId()).get();
        passenger.getBookedTickets().add(tck);

        //And the end return the ticketId that has come from db

        trainRepository.save(trains);

        return ticketRepository.save(tck).getTicketId();

    }
}
