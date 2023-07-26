package com.microservices.discoveryserver.repository;


import com.microservices.discoveryserver.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order,Long> {



}
