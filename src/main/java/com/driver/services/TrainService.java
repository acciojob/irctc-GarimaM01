package com.driver.services;

import com.driver.EntryDto.AddTrainEntryDto;
import com.driver.EntryDto.SeatAvailabilityEntryDto;
import com.driver.model.Passenger;
import com.driver.model.Station;
import com.driver.model.Ticket;
import com.driver.model.Train;
import com.driver.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

@Service
public class TrainService {

    @Autowired
    TrainRepository trainRepository;
    List<Train>trains=new ArrayList<>();
    public Integer addTrain(AddTrainEntryDto trainEntryDto){

        //Add the train to the trainRepository
        Train train = new Train();
        train.setNoOfSeats(trainEntryDto.getNoOfSeats());


        //and route String logic to be taken from the Problem statement.
        List<Station> list = trainEntryDto.getStationRoute();
        String route = "";

        for(int i=0;i<list.size();i++){
            if(i==list.size()-1)
                route += list.get(i);
            else
                route += list.get(i) + ",";
        }
        train.setRoute(route);

        //Save the train and return the trainId that is generated from the database.
        //Avoid using the lombok library
        train.setDepartureTime(trainEntryDto.getDepartureTime());
        trains.add(train);
        return trainRepository.save(train).getTrainId();
    }

    public Integer calculateAvailableSeats(SeatAvailabilityEntryDto seatAvailabilityEntryDto){

        //Calculate the total seats available
        Train train=trainRepository.findById(seatAvailabilityEntryDto.getTrainId()).get();

        //Suppose the route is A B C D
        //And there are 2 seats avaialble in total in the train
        //and 2 tickets are bookedRoute from A to C and B to D.
        //The seat is available only between A to C and A to B. If a seat is empty between 2 station it will be counted to our final result
        //even if that seat is bookedRoute post the destStation or before the boardingStation
        //Inshort : a train has totalNo of seats and there are tickets from and to different locations
        //We need to find out the available seats between the given 2 stations.

        List<Ticket>ticketList=train.getBookedTickets();
        String []trainRoot=train.getRoute().split(",");
        HashMap<String,Integer> map=new HashMap<>();
        for(int i=0;i<trainRoot.length;i++){
            map.put(trainRoot[i],i);
        }
        if(!map.containsKey(seatAvailabilityEntryDto.getFromStation().toString())||!map.containsKey(seatAvailabilityEntryDto.getToStation().toString())){
            return 0;
        }
        int bookedRoute=0;
        for(Ticket ticket:ticketList){
            bookedRoute+=ticket.getPassengersList().size();
        }
        int cnt=train.getNoOfSeats()-bookedRoute;
        for(Ticket t:ticketList){
            String fromStation=t.getFromStation().toString();
            String toStation=t.getToStation().toString();
            if(map.get(seatAvailabilityEntryDto.getToStation().toString())<=map.get(fromStation)){
                cnt++;
            }
            else if (map.get(seatAvailabilityEntryDto.getFromStation().toString())>=map.get(toStation)){
                cnt++;
            }
        }
        return cnt+2;
    }

    public Integer calculatePeopleBoardingAtAStation(Integer trainId,Station station) throws Exception{

        //We need to find out the number of people who will be boarding a train from a particular station
        Train train=trainRepository.findById(trainId).get();
        String reqStation=station.toString();
        String arr[]=train.getRoute().split(",");
        boolean noPass=false;

        for(String people:arr){
            if(people.equals(reqStation)){
                noPass=true;
                break;
            }
        }

        //if the trainId is not passing through that station
        //throw new Exception("Train is not passing from this station");
        if(noPass==false){
            throw new Exception("Train is not passing from this station");
        }

        //  in a happy case we need to find out the number of such people.
        int noOfPeople=0;
        List<Ticket>ticketList= train.getBookedTickets();
        for(Ticket ticket:ticketList){
            if(ticket.getFromStation().toString().equals(reqStation)){
                noOfPeople+=ticket.getPassengersList().size();
            }
        }

        return noOfPeople;
    }

    public Integer calculateOldestPersonTravelling(Integer trainId){

        //Throughout the journey of the train between any 2 stations
        //We need to find out the oldPassenger of the oldest person that is travelling the train
        //If there are no people travelling in that train you can return 0

        Train train=trainRepository.findById(trainId).get();
        int oldPassenger= Integer.MIN_VALUE;
        if(train.getBookedTickets().size()==0)
            return 0;

        List<Ticket>ticketList=train.getBookedTickets();
        for(Ticket ticket:ticketList){
            List<Passenger>passengers=ticket.getPassengersList();
            for(Passenger passenger:passengers){
                oldPassenger=Math.max(oldPassenger,passenger.getAge());
            }
        }
        return oldPassenger;
    }

    public List<Integer> trainsBetweenAGivenTime(Station station, LocalTime startTime, LocalTime endTime){

        //When you are at a particular station you need to find out the number of trains that will pass through a given station
        //between a particular time frame both start time and end time included.
        //You can assume that the date change doesn't need to be done ie the travel will certainly happen with the same date (More details
        //in problem statement)
        //You can also assume the seconds and milli seconds value will be 0 in a LocalTime format.

        List<Integer> listOfTrain = new ArrayList<>();
        List<Train> trains = trainRepository.findAll();
        for(Train t:trains){
            String str = t.getRoute();
            String[] result = str.split(",");
            for(int i=0;i<result.length;i++){
                if(Objects.equals(result[i], String.valueOf(station))){
                    int startTrainMin = (startTime.getHour() * 60) + startTime.getMinute();
                    int lastTrainInMin = (endTime.getHour() * 60) + endTime.getMinute();


                    int departuredTimeInMin = (t.getDepartureTime().getHour() * 60) + t.getDepartureTime().getMinute();
                    int arrivalTimeInMin  = departuredTimeInMin + (i * 60);
                    if(arrivalTimeInMin>=startTrainMin && arrivalTimeInMin<=lastTrainInMin)
                        listOfTrain.add(t.getTrainId());
                }
            }
        }
        return listOfTrain;
    }

}
