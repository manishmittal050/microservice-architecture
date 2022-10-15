package com.mmittal.productservice.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {
	
	@GetMapping("check")
	public ResponseEntity<String> getProducts(){
		return new ResponseEntity<String>("Acccessed!!!!",HttpStatus.OK);
	}

}
