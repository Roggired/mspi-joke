package ru.kefungus.joke;

import java.io.IOException;

public class App {
    public static void main(String[] args) throws InterruptedException, IOException {
        String message = "Здравствуйте, я хочу сдать 4 лабку.";
        String studentName = "Петя";
        System.out.println("Внимание, история.");
        System.in.read();
        System.out.println("Приходит студент " + studentName + " к Алексею Евгеньевичу в среду в 10.00 и говорит:");
        System.out.println(message);
    }
}
