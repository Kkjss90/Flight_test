package org.example;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class TicketAnalyzer {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java TicketAnalyzer <path_to_tickets.json>");
            System.exit(1);
        }

        String filePath = args[0];
        Path filePathObj = Paths.get(filePath);

        try {

            if (!Files.exists(filePathObj)) {
                System.err.println("Ошибка: Файл не найден - " + filePath);
                System.err.println("Убедитесь, что файл существует и путь указан правильно");
                System.exit(1);
            }

            if (Files.isDirectory(filePathObj)) {
                System.err.println("Ошибка: Указанный путь является директорией, а не файлом - " + filePath);
                System.exit(1);
            }

            if (!Files.isReadable(filePathObj)) {
                System.err.println("Ошибка: Нет прав на чтение файла - " + filePath);
                System.exit(1);
            }
            byte[] fileBytes = Files.readAllBytes(filePathObj);

            if (fileBytes.length >= 3 &&
                    fileBytes[0] == (byte) 0xEF &&
                    fileBytes[1] == (byte) 0xBB &&
                    fileBytes[2] == (byte) 0xBF) {
                fileBytes = Arrays.copyOfRange(fileBytes, 3, fileBytes.length);
            }

            String text = new String(fileBytes, StandardCharsets.UTF_8);

            if (text.trim().isEmpty()) {
                System.err.println("Ошибка: Файл пустой - " + filePath);
                System.exit(1);
            }

            JSONObject jsonObject = new JSONObject(text);
            JSONArray ticketsArray = jsonObject.getJSONArray("tickets");

            List<Ticket> vvoTlvTickets = new ArrayList<>();
            Map<String, List<Long>> carrierFlightTimes = new HashMap<>();

            for (int i = 0; i < ticketsArray.length(); i++) {
                JSONObject ticketJson = ticketsArray.getJSONObject(i);
                Ticket ticket = parseTicket(ticketJson);

                if ("VVO".equals(ticket.origin) && "TLV".equals(ticket.destination)) {
                    vvoTlvTickets.add(ticket);

                    long flightTimeMinutes = calculateFlightTimeSafe(ticket);

                    carrierFlightTimes
                            .computeIfAbsent(ticket.carrier, k -> new ArrayList<>())
                            .add(flightTimeMinutes);
                }
            }

            System.out.println("Минимальное время полета для каждого авиаперевозчика:");
            for (Map.Entry<String, List<Long>> entry : carrierFlightTimes.entrySet()) {
                long minTime = Collections.min(entry.getValue());
                long hours = minTime / 60;
                long minutes = minTime % 60;
                System.out.printf("%s: %d часов %d минут%n", entry.getKey(), hours, minutes);
            }

            System.out.println();

            List<Integer> prices = new ArrayList<>();
            for (Ticket ticket : vvoTlvTickets) {
                prices.add(ticket.price);
            }

            Collections.sort(prices);
            double averagePrice = prices.stream().mapToInt(Integer::intValue).average().orElse(0);
            double medianPrice = calculateMedian(prices);
            double difference = averagePrice - medianPrice;

            System.out.printf("Количество рейсов Владивосток-Тель-Авив: %d%n", vvoTlvTickets.size());
            System.out.printf("Средняя цена: %.2f руб.%n", averagePrice);
            System.out.printf("Медианная цена: %.2f руб.%n", medianPrice);
            System.out.printf("Разница между средней ценой и медианой: %.2f руб.%n", difference);

        } catch (IOException | JSONException e) {
            System.err.println("Ошибка при обработке файла: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static long calculateFlightTimeSafe(Ticket ticket) {
        try {
            String departureTime = ticket.departureTime.length() == 4 ? "0" + ticket.departureTime : ticket.departureTime;
            String arrivalTime = ticket.arrivalTime.length() == 4 ? "0" + ticket.arrivalTime : ticket.arrivalTime;

            String[] departureParts = departureTime.split(":");
            String[] arrivalParts = arrivalTime.split(":");

            int departureHour = Integer.parseInt(departureParts[0]);
            int departureMinute = Integer.parseInt(departureParts[1]);
            int arrivalHour = Integer.parseInt(arrivalParts[0]);
            int arrivalMinute = Integer.parseInt(arrivalParts[1]);

            String[] depDateParts = ticket.departureDate.split("\\.");
            String[] arrDateParts = ticket.arrivalDate.split("\\.");

            int depDay = Integer.parseInt(depDateParts[0]);
            int depMonth = Integer.parseInt(depDateParts[1]);
            int depYear = 2000 + Integer.parseInt(depDateParts[2]);

            int arrDay = Integer.parseInt(arrDateParts[0]);
            int arrMonth = Integer.parseInt(arrDateParts[1]);
            int arrYear = 2000 + Integer.parseInt(arrDateParts[2]);

            LocalDateTime departure = LocalDateTime.of(depYear, depMonth, depDay, departureHour, departureMinute);
            LocalDateTime arrival = LocalDateTime.of(arrYear, arrMonth, arrDay, arrivalHour, arrivalMinute);

            return Duration.between(departure, arrival).toMinutes();

        } catch (Exception e) {
            System.err.println("Ошибка при расчете времени полета для билета: " + ticket);
            e.printStackTrace();
            return 0;
        }
    }

    private static Ticket parseTicket(JSONObject json) {
        Ticket ticket = new Ticket();
        ticket.origin = json.getString("origin");
        ticket.originName = json.getString("origin_name");
        ticket.destination = json.getString("destination");
        ticket.destinationName = json.getString("destination_name");
        ticket.departureDate = json.getString("departure_date");
        ticket.departureTime = json.getString("departure_time");
        ticket.arrivalDate = json.getString("arrival_date");
        ticket.arrivalTime = json.getString("arrival_time");
        ticket.carrier = json.getString("carrier");
        ticket.stops = json.getInt("stops");
        ticket.price = json.getInt("price");
        return ticket;
    }

    private static double calculateMedian(List<Integer> list) {
        int size = list.size();
        if (size == 0) return 0;
        if (size % 2 == 0) {
            return (list.get(size / 2 - 1) + list.get(size / 2)) / 2.0;
        } else {
            return list.get(size / 2);
        }
    }

    static class Ticket {
        String origin;
        String originName;
        String destination;
        String destinationName;
        String departureDate;
        String departureTime;
        String arrivalDate;
        String arrivalTime;
        String carrier;
        int stops;
        int price;

        @Override
        public String toString() {
            return String.format("%s %s %s -> %s %s %s [%s]",
                    origin, departureDate, departureTime,
                    destination, arrivalDate, arrivalTime, carrier);
        }
    }
}