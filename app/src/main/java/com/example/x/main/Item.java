package com.example.x.main;

public class Item {

    private int id;
    private String car_brand;
    private String type_of_car;
    private int carrying_capacity;
    private String state_number;

    public Item(int id, String car_brand, String type_of_car,
                               int carrying_capacity, String state_number){
        this.id = id;
        this.car_brand = car_brand;
        this.type_of_car = type_of_car;
        this.carrying_capacity = carrying_capacity;
        this.state_number = state_number;
    }

    public int getId(){
        return id;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(car_brand).append("\n");
        sb.append("Тип: ").append(type_of_car).append("\n");
        sb.append("Грузоподъемность: ").append(carrying_capacity).append("\n");
        sb.append("Номер: ").append(state_number).append("\n");

        return sb.toString();
    }
}
