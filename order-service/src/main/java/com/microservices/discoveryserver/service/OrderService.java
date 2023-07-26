package com.microservices.discoveryserver.service;

import com.microservices.discoveryserver.dto.InventoryResponse;
import com.microservices.discoveryserver.dto.OrderLineItemsDto;
import com.microservices.discoveryserver.dto.OrderRequest;
import com.microservices.discoveryserver.model.Order;
import com.microservices.discoveryserver.model.OrderLineItems;
import com.microservices.discoveryserver.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public OrderService(OrderRepository orderRepository, WebClient.Builder webClientBuilder) {
        this.orderRepository = orderRepository;

        this.webClientBuilder = webClientBuilder;
    }

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsList().stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemsList(orderLineItems);
        // orderRequest -----> order.OrderLineItems

        List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();

        // Call Inventory Service and place order if product is in stock
        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                                 .uri("http://inventory-service/api/inventory",
                                         uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                                 .retrieve()
                                 .bodyToMono(InventoryResponse[].class)
                                 .block();// makes it Synchronized

        boolean allProductsInStock = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::getIsInStock);
        // matches inventoryResponseArray which we took it from InventoryService Get Request
        // and InventoryResponse's getIsInStock

        if (allProductsInStock) {
            orderRepository.save(order);
        } else {
            throw new IllegalArgumentException("Product is not in stock at the moment...");
        }

    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;

    }

}
