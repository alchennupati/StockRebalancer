package com.aptos.coding;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ConnectionSpec;
import okhttp3.TlsVersion;
import okhttp3.CipherSuite;
import okhttp3.CertificatePinner;
import javax.net.ssl.SSLException;

import javax.net.ssl.SSLContext;


import okhttp3.ConnectionSpec.Builder;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import org.w3c.dom.Document;


public class StockManager {

	private static final Map<String, Integer> PORTFOLIO_PERCENTAGE = new HashMap<>();

	static {
		PORTFOLIO_PERCENTAGE.put("AAPL", 22);
		PORTFOLIO_PERCENTAGE.put("GOOG", 38);
		PORTFOLIO_PERCENTAGE.put("ACAD", 15);
		PORTFOLIO_PERCENTAGE.put("GFN", 25);
	}

	public static void main(String[] args) {

		//Calculate the initial balance
		initialBalance();

		//Calculate the starting percentage of stocks
		StockOperator.calculateActualStockPortfolioPercentage();

		//Re-balance portfolio every 15 minutes
		long lastCall = 0;
		while(true) {
			if(System.currentTimeMillis() - lastCall > 900000) {
				lastCall = System.currentTimeMillis();
				StockOperator.rebalance();
			}
		}
	}

	private static void initialBalance() {

		StockOperator.addStock("AAPL", 50);
		StockOperator.addStock("GOOG", 200);
		StockOperator.addStock("CYBR", 150);
		StockOperator.addStock("ABB", 900);
		StockOperator.addStock("GFN", 0);
		StockOperator.addStock("ACAD", 0);
	}

	// Class to perform  stock operations
	static class StockOperator {

		private static final Map<String, Stock> stocks = new HashMap<>();
		private static int totalStocks = 0; // Number of stocks
		private static double totalValue = 0.0; // Value of the stock in currency

		enum OP {
			SELL, BUY
		};


		// First time initiate the portfolio
		public static void addStock(String symbol, int quantity) {
			int targetPortfolioPercentage = null != PORTFOLIO_PERCENTAGE.get(symbol) ? PORTFOLIO_PERCENTAGE.get(symbol)
					: 0;

			//get the price of the stock
			Double currentPrice = HttpUtil.run(symbol);

			// Actual Portfolio % will be updated later during re-balance
			stocks.put(symbol, new Stock(symbol, quantity, currentPrice ,targetPortfolioPercentage, 0));
			StockOperator.totalValue += stocks.get(symbol).getValue();
		}

		public static void calculateActualStockPortfolioPercentage() {

			for (String symbol : stocks.keySet()) {

				Stock symbolStock = stocks.get(symbol);

				symbolStock.setActualPortfolioPercentage(symbolStock.getValue() * 100 / totalValue);

				if(symbolStock.getShares() > 0)
					System.out.println(symbolStock);
			}
		}

		public static void rebalance() {

			for (String symbol : stocks.keySet()) {
				transact(symbol);
			}

			calculateActualStockPortfolioPercentage();
		}

		// Determine how many stocks to buy or sell
		public static void transact(String symbol) {

			Stock symbolStock = stocks.get(symbol);

			if(PORTFOLIO_PERCENTAGE.containsKey(symbolStock.getSymbol())){
				if (symbolStock.getTargetPortfolioPercentage() > symbolStock.getActualPortfolioPercentage()) {
	
					double deficitPercentage = symbolStock.getTargetPortfolioPercentage()
							- symbolStock.getActualPortfolioPercentage();
	
					double deficitValue = totalValue*deficitPercentage/100;
					int stocksToBuy = 0;
					if(symbolStock.getPrice() > 0.0)
						 stocksToBuy = (int) (deficitValue / symbolStock.getPrice());
					else
						stocksToBuy = (int) (deficitValue; //Exception scenario when no stock price available. Assuming price 1, cash?
					System.out.println(" stocksToBuy "+stocksToBuy);
					execute(OP.BUY, symbol, stocksToBuy);
	
				} else {
					double surplusPercentage = symbolStock.getActualPortfolioPercentage()
							- symbolStock.getTargetPortfolioPercentage();

					double deficitValue = totalValue*surplusPercentage/100;
					int stocksToSell = (int) (deficitValue / symbolStock.getPrice());
					System.out.println(" stocksToSell "+stocksToSell);
					execute(OP.SELL, symbol, stocksToSell);
				}
			}else{
				// Not in the initial original list, however, in the desired list
				int stocksToSell = symbolStock.getShares();
				if(stocksToSell > 0)
					execute(OP.SELL, symbol, stocksToSell);
			}

		}

		// Execute Buy or Sell
		private static void execute(OP op, String symbol, int quantity) {
			switch (op) {
			case BUY:
				Stock symbolStockBuy = stocks.get(symbol);
				int stockTotalAfterBuy = symbolStockBuy.getShares() + quantity;
				symbolStockBuy.setShares(stockTotalAfterBuy);
				break;
			case SELL:
				Stock symbolStockSell = stocks.get(symbol);
				int stockTotalAfterSell = symbolStockSell.getShares() - quantity;
				symbolStockSell.setShares(stockTotalAfterSell);
				break;
			}
		}

	}

	// Stock Object to hold stock info
	static class Stock {

		private String symbol;

		private int shares;
		
		public double price;

		private double targetPortfolioPercentage;

		private double actualPortfolioPercentage;

		public Stock(String symbol, int shares, double price, double targetPortfolioPercentage, double actualPortfolioPercentage) {
			this.symbol = symbol;
			this.shares = shares;
			this.price = price;
			this.targetPortfolioPercentage = targetPortfolioPercentage;
			this.actualPortfolioPercentage = actualPortfolioPercentage;
		}
		
		public double getValue() {
	        return this.price * this.shares;
	    }

		public String getSymbol() {
			return symbol;
		}

		public void setSymbol(String symbol) {
			this.symbol = symbol;
		}

		public int getShares() {
			return shares;
		}

		public void setShares(int shares) {
			this.shares = shares;
		}

		public double getPrice() {
			return price;
		}

		public void setPrice(double price) {
			this.price = price;
		}

		public double getTargetPortfolioPercentage() {
			return targetPortfolioPercentage;
		}

		public void setTargetPortfolioPercentage(double targetPortfolioPercentage) {
			this.targetPortfolioPercentage = targetPortfolioPercentage;
		}

		public double getActualPortfolioPercentage() {
			return actualPortfolioPercentage;
		}

		public void setActualPortfolioPercentage(double actualPortfolioPercentage) {
			this.actualPortfolioPercentage = actualPortfolioPercentage;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Stock [symbol=");
			builder.append(symbol);
			builder.append(", shares=");
			builder.append(shares);
			builder.append(", price=");
			builder.append(price);	
			builder.append(", value=");
			builder.append(this.getValue());
			builder.append(", targetPortfolioPercentage=");
			builder.append(targetPortfolioPercentage);
			builder.append(", actualPortfolioPercentage=");
			builder.append(actualPortfolioPercentage);
			builder.append("]");
			return builder.toString();
		}

	}

	// OKHttp Client
	public final static class HttpUtil{
		private static final String url = "https://www.alphavantage.co/query?apikey=BPYXEC00NPEI4YSY&function=TIME_SERIES_DAILY_ADJUSTED";

		static final OkHttpClient client = new OkHttpClient.Builder()
	    .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
	    .build();

		public static  double run(String symbol)  {
		        try {
		            Request request = new Request.Builder()
		        	         .url(url+"&symbol="+symbol)
		                    .build();                
		            Response response = client.newCall(request).execute();
		            String output = response.body().string();
					ObjectMapper mapper = new ObjectMapper();
					// convert JSON string to Map
					Map<String, String> map = mapper.readValue(output, Map.class);
					Map<String, Object> object = mapper.readValue(output, new TypeReference<Map<String,Object>>(){});
					for(Map.Entry<String, Object> entry : object.entrySet()){
						if(entry.getKey().equalsIgnoreCase("Time Series (Daily)")){
							String stockpriceInfo = entry.getValue().toString();
							double currentPrice = Double.parseDouble(stockpriceInfo.substring(stockpriceInfo.indexOf("close=")+6,stockpriceInfo.indexOf(", 5. adjusted close=")));
							return currentPrice;
						}
					}
		            return 0.0;
		        } catch (java.io.IOException e) { 
		        	System.out.println("respone"+ e.toString());
		            return 0.0;
		        }
		    }
	}
	 
	 

}