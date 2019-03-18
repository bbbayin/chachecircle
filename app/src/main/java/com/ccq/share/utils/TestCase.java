package com.ccq.share.utils;

import java.util.ArrayList;
import java.util.List;

public class TestCase {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        List<String> alterList = alterList(list);
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i));
        }
        for (int i = 0; i < alterList.size(); i++) {
            System.out.println("alter  :"+alterList.get(i));
        }
    }

    private static List<String> alterList(List<String> list) {
        for (int i = 0; i < 10; i++) {
            list.add(i + "aaa");
        }
        return list;
    }
}
