package com.debanjan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MedicineManagementApplication {

	public static void main(String[] args) {
		if (java.util.Arrays.asList(args).contains("--desktop")) {
			com.debanjan.desktop.DesktopApp.launch();
			return;
		}
		SpringApplication.run(MedicineManagementApplication.class, args);
	}

}
