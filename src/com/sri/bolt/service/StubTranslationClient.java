package com.sri.bolt.service;

public class StubTranslationClient implements ServiceClient {

	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reinit() {
	}

	public String translateText(String text) {
		return "Stub translation!";
	}

	@Override
	public void cleanup() {
		// TODO Auto-generated method stub
		
	}

}
