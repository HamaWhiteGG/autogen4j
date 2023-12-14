package com.hw.autogen4j.example;

import java.util.Scanner;

/**
 * @author HamaWhite
 */
public class Test {

    public static void main(String[] args) {
        System.out.print("请输入下一步操作: ");
        Scanner scanner = new Scanner(System.in);
        String result=scanner.nextLine();

        System.out.print("结果:");
        System.out.print(result);
        System.out.print("结束");
    }
}
