
package com.crio.warmup.stock.portfolio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {




  private RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will
  // break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  private StockQuotesService stockQuotesService;

  protected PortfolioManagerImpl(StockQuotesService stockQuotesService){
    this.stockQuotesService = stockQuotesService;
  }
 
  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF
  



  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException, StockQuoteServiceException {

        return stockQuotesService.getStockQuote(symbol, from, to);      
  }

  // public List<Candle> getStockQuote(String symbol , LocalDate startLocalDate , LocalDate endLocalDate) throws JsonProcessingException{
  //   return stockQuotesService.getStockQuote(symbol, startLocalDate, endLocalDate);
  // }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String token = "bf9fc8bfe29b6280578e504ee5a7c951488b88b0";   
    
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";

    String url = uriTemplate.replace("$APIKEY" , token).replace("$SYMBOL" , symbol).replace("$STARTDATE", startDate.toString()).replace("$ENDDATE", endDate.toString());

    return url;
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate)
      throws StockQuoteServiceException {
    // TODO Auto-generated method stub
    AnnualizedReturn annualizedReturn;
    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();

    for(int i = 0; i < portfolioTrades.size(); i++){

      annualizedReturn = getAnnualizedReturn(portfolioTrades.get(i) , endDate);
      annualizedReturns.add(annualizedReturn);
    }

    Comparator<AnnualizedReturn> SortByAnnReturn =Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
    Collections.sort(annualizedReturns , SortByAnnReturn);
    return annualizedReturns;
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate, int numThreads) throws InterruptedException, StockQuoteServiceException {
    // TODO Auto-generated method stub

    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
    List<Future<AnnualizedReturn>> futureReturnsList = new ArrayList<Future<AnnualizedReturn>>();
    final ExecutorService pool = Executors.newFixedThreadPool(numThreads);

    for (int i = 0; i < portfolioTrades.size(); i++) {
      PortfolioTrade trade = portfolioTrades.get(i);
      Callable<AnnualizedReturn> callableTask = () -> {
        return getAnnualizedReturn(trade, endDate);
      };
      Future<AnnualizedReturn> futureReturns = pool.submit(callableTask);
      futureReturnsList.add(futureReturns);
    }
    for (int i = 0; i < portfolioTrades.size(); i++) {
      Future<AnnualizedReturn> futureReturns = futureReturnsList.get(i);
      try {
        AnnualizedReturn returns = futureReturns.get();
        annualizedReturns.add(returns);
      } catch (ExecutionException e) {
        throw new StockQuoteServiceException("Error when calling the API", e);  
      }
    }
    Comparator<AnnualizedReturn> SortByAnnReturn = Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();    
    Collections.sort(annualizedReturns, SortByAnnReturn);
    return annualizedReturns;
  }

  public AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade, LocalDate endDate)
      throws StockQuoteServiceException {
   LocalDate startDate = trade.getPurchaseDate();
   String symbol = trade.getSymbol();

   Double buyPrice = 0.0 , sellPrice = 0.0;

   try{
     LocalDate startLocalDate = trade.getPurchaseDate();
     List<Candle> stocksStartToEndFull = getStockQuote(symbol, startLocalDate, endDate);
     Collections.sort(stocksStartToEndFull , (candle1 , candle2) -> {
       return candle1.getDate().compareTo(candle2.getDate());
     });
     Candle stockStartDate = stocksStartToEndFull.get(0);
     Candle stocksLatest = stocksStartToEndFull.get(stocksStartToEndFull.size() - 1);

     buyPrice = stockStartDate.getOpen();
     sellPrice = stocksLatest.getClose();
     endDate = stocksLatest.getDate();     
   }
   catch(JsonProcessingException e){
     throw new RuntimeException(e);
   }
   Double totalReturn = (sellPrice - buyPrice) / buyPrice;

   Double numYears = (double)ChronoUnit.DAYS.between(startDate, endDate) / 365.24 ;

   Double annualizedReturns = Math.pow((1+totalReturn) , (1/numYears)) - 1;

   return new AnnualizedReturn(symbol, annualizedReturns, totalReturn);
  }  
}
