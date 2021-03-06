package com.example.mymap.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Dao
public interface MyDAO {

    @Insert
    long insertTrip(Trip trip);     //return rowId

    @Query("SELECT tripId FROM trip WHERE rowid = :rowId")
    int getTripIdFromRowId(long rowId);

    @Insert
    void insertPhoto(TripPhoto photo);

    @Insert
    void insertTripLocations(TripLocation tripLocation);

    @Query("SELECT * FROM trip")
    List<Trip> getListTrip();

    @Query("SELECT tripName FROM trip WHERE tripId = :tripId")
    String getTripName(int tripId);

    @Query("SELECT * FROM trip WHERE tripId = :tripId")
    Trip getTrip(int tripId);

    @Query("SELECT photoPath FROM photo WHERE tripBelongId = :tripId")
    List<String> getListPhotoPathFromTrip(int tripId);

    @Query("SELECT * FROM trip_location WHERE tripBelongId = :tripId")
    List<TripLocation> getListTripLocationFromTrip(int tripId);

    @Query("UPDATE trip_location SET timePassed = :time " +
            "WHERE tripBelongId = :tripId AND locationId = :locationId")
    void updateTimePassed(int tripId, int locationId, Date time);

    @Update
    void updateTrip(Trip trip);

    @Update
    void updateTripLocation(TripLocation tripLocation);

    @Query("DELETE FROM trip where tripId = :tripId")
    void deleteTrip(int tripId);

    @Query("DELETE FROM photo WHERE tripBelongId = :tripId")
    void deleteAllPhotosOfTrip(int tripId);

    @Query("DELETE FROM trip_location WHERE tripBelongId = :tripId")
    void deleteAllLocationsOfTrip(int tripId);





}
