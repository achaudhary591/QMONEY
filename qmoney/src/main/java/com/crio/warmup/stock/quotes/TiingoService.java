
package com.crio.warmup.stock.quotes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import javax.management.RuntimeErrorException;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.exception.NullArgumentException;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

  public static final String TOKEN = "bf9fc8bfe29b6280578e504ee5a7c951488b88b0";
  private RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException , StockQuoteServiceException {
    // TODO Auto-generated method stub
    List<Candle> stocksStartToEndDate;
    
    if(from.compareTo(to) >= 0){
      throw new RuntimeErrorException(null);
    }
    String url = buildUri(symbol, from , to);
    
    try{
      String stocks = restTemplate.getForObject(url, String.class);
      ObjectMapper objectMapper = getObjectMapper();

      Candle[] stocksStartToEndDateArray = objectMapper.readValue(stocks , TiingoCandle[].class);

      if(stocksStartToEndDateArray != null){
        stocksStartToEndDate = Arrays.asList(stocksStartToEndDateArray);
      }
      else{
        stocksStartToEndDate = Arrays.asList(new TiingoCandle[0]);
      }
    }
    catch(NullPointerException e){
      throw new StockQuoteServiceException("Error occureed when requesting response from Tiingo API" , e.getCause());
    }

    return stocksStartToEndDate;
  }
  private static ObjectMapper getObjectMapper() {
    ObjectMapper om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    return om;
  }
  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String token = "bf9fc8bfe29b6280578e504ee5a7c951488b88b0";   
    
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";

    String url = uriTemplate.replace("$APIKEY" , token).replace("$SYMBOL" , symbol).replace("$STARTDATE", startDate.toString()).replace("$ENDDATE", endDate.toString());

    return url;
  }


  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement getStockQuote method below that was also declared in the interface.

  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  //    ./gradlew test --tests TiingoServiceTest


  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.

}
