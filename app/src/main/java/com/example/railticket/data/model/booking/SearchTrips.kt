package com.example.railticket.data.model.booking

data class SearchTripsResponse(
    val data: SearchTripsData? = null,
    val extra: Map<String, String>? = null
)

data class SearchTripsData(
    val trains: List<TrainTrip>? = null,
    val recommendations: List<Any>? = null,
    val selected_seat_class: String? = null
)

data class TrainTrip(
    val trip_number: String? = null,
    val departure_date_time: String? = null,
    val departure_full_date: String? = null,
    val departure_date_time_jd: String? = null,
    val arrival_date_time: String? = null,
    val travel_time: String? = null,
    val origin_city_name: String? = null,
    val destination_city_name: String? = null,
    val seat_types: List<SeatType>? = null,
    val train_model: String? = null,
    val is_open_for_all: Boolean? = null,
    val is_eid_trip: Int? = null,
    val parent_route: ParentRoute? = null,
    val boarding_points: List<BoardingPoint>? = null,
    val is_international: Int? = null,
    val is_from_city_international: Boolean? = null
)

data class SeatType(
    val key: Int? = null,
    val type: String? = null,
    val trip_id: Long? = null,
    val trip_route_id: Long? = null,
    val route_id: Long? = null,
    val fare: String? = null,
    val vat_percent: Int? = null,
    val vat_amount: Int? = null,
    val origin_city_seq: Int? = null,
    val destination_city_seq: Int? = null,
    val seat_counts: SeatCounts? = null
)

data class SeatCounts(
    val online: Int? = null,
    val offline: Int? = null,
    val is_divided: Boolean? = null
)

data class ParentRoute(
    val origin_city_name: String? = null,
    val destination_city_name: String? = null,
    val departure_date: String? = null,
    val departure_time: String? = null,
    val arrival_date: String? = null,
    val arrival_time: String? = null
)

data class BoardingPoint(
    val trip_point_id: Long? = null,
    val location_id: Int? = null,
    val location_name: String? = null,
    val location_time: String? = null,
    val location_date: String? = null
)
