package com.swishsales.concurrent.repository;

import com.swishsales.concurrent.entity.Item;
import com.swishsales.concurrent.entity.ItemBasketball;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ItemRepository {

    private final Map<String, Item> itemsTable = new HashMap<>();

    public ItemRepository() {
        itemsTable.put("101", new ItemBasketball("101", "Spalding TF-1000", "Oficial indoor", 7, "Spalding", "TF-1000 Legacy"));
        itemsTable.put("102", new ItemBasketball("102", "Wilson Evolution", "A mais usada no high school", 7, "Wilson", "Evolution"));
        itemsTable.put("103", new ItemBasketball("103", "Nike Dominate", "Boa pra jogar outdoor", 7, "Nike", "Dominate 8P"));
    }

    public Item findById(String id) {
        return itemsTable.get(id);
    }

    // Used by the producer thread to select a random item
    public Item getRandomItem() {
        List<Item> values = new ArrayList<>(itemsTable.values());
        return values.get(new Random().nextInt(values.size()));
    }
}