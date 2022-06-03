
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {



public static RestTemplate restTemplate = new RestTemplate();
public static PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    return candles.get(0).getOpen();
 }


 public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.get(candles.size()-1).getClose();
 }

 public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
    
  RestTemplate restTemplate = new RestTemplate();
  String url = prepareUrl(trade, endDate, token);

  TiingoCandle[] quotes = restTemplate.getForObject(url, TiingoCandle[].class);

  return Arrays.asList(quotes);
}

public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
  String url = "https://api.tiingo.com/tiingo/daily/" + trade.getSymbol() + "/prices" + "?" + "startDate="
      + trade.getPurchaseDate() + "&endDate=" + endDate + "&token=" + token;
  return url;
}


  
  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
       String file = args[0];
       LocalDate endDate = LocalDate.parse(args[1]);
       String contents = readFileAsString(file);
       ObjectMapper objectMapper = getObjectMapper();

       PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents , PortfolioTrade[].class);
       return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }

  private static String readFileAsString(String filename) throws URISyntaxException, IOException { 
  
    return new String(Files.readAllBytes(resolveFileFromResources(filename).toPath()), "UTF-8"); 
    }


  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());
    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }


  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }  
 

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    ObjectMapper om = getObjectMapper();
    File file = resolveFileFromResources(args[0]);
    PortfolioTrade[] trade = om.readValue(file, PortfolioTrade[].class);
    List<String> symbols = new ArrayList<String>();
    for (PortfolioTrade t : trade) {
      symbols.add(t.getSymbol());

    }
    return symbols;
  }


  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }
 

  private static ObjectMapper getObjectMapper() {
    ObjectMapper om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    return om;
    

  }


  public static String getToken() {
    return TOKEN;
  }

  public static final String TOKEN = "bf9fc8bfe29b6280578e504ee5a7c951488b88b0";

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {

        List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
        LocalDate endLocalDate = LocalDate.parse(args[1]);
        
        File trades = resolveFileFromResources(args[0]);
        ObjectMapper objectMapper = getObjectMapper();

        PortfolioTrade[] tradesJsons = objectMapper.readValue(trades, PortfolioTrade[].class);

        for(int i = 0 ; i < tradesJsons.length ; i++){
          annualizedReturns.add(getAnnualizedReturn(tradesJsons[i] , endLocalDate));
        }
        
        Comparator<AnnualizedReturn> SortByAnnReturn = Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
        Collections.sort(annualizedReturns , SortByAnnReturn);

     return annualizedReturns;
  }

  private static AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade, LocalDate endLocalDate) {

    String ticker = trade.getSymbol();
    LocalDate startLocalDate = trade.getPurchaseDate();

    if(startLocalDate.compareTo(endLocalDate) >= 0){
      throw new RuntimeException();
    }

    String url = String.format("https://api.tiingo.com/tiingo/daily/%s/prices?"+"startDate=%s&endDate=%s&token=%s", ticker , startLocalDate.toString() , endLocalDate.toString() , TOKEN);

    RestTemplate restTemplate = new RestTemplate();

    TiingoCandle[] stocksStartToEndDate =  restTemplate.getForObject(url , TiingoCandle[].class);

    if(stocksStartToEndDate != null){
      TiingoCandle stockStartDate = stocksStartToEndDate[0];
      TiingoCandle stockLatest = stocksStartToEndDate[stocksStartToEndDate.length - 1];

      double buyPrice = stockStartDate.getOpen();
      double sellPrice = stockLatest.getClose();

      AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(endLocalDate, trade, buyPrice, sellPrice);
      return annualizedReturn;
    }
    else{
      return new AnnualizedReturn(ticker, Double.NaN, Double.NaN);
    }
    
  }

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
     String resultOfResolveFilePathArgs0 = "/home/crio-user/workspace/achaudhary591-ME_QMONEY_V2/qmoney/bin/main/trades.json";
     String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@5552768b";
     String functionNameFromTestFileInStackTrace = "PortfolioManagerApplicationTest.mainReadFile()";
     String lineNumberFromTestFileInStackTrace = "27:1";


    return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
        toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
        lineNumberFromTestFileInStackTrace});

  }


  public static AnnualizedReturn calculateAnnualizedReturns(
    LocalDate endDate,
    PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    
      double totalReturn = (sellPrice - buyPrice) / buyPrice ;
    

      double total_num_years = ChronoUnit.DAYS.between(trade.getPurchaseDate() , endDate) / 365.24;
      
      double annualized_returns = Math.pow((1 + totalReturn) , (1 / total_num_years))-1;
    return new AnnualizedReturn(trade.getSymbol() , annualized_returns , totalReturn);
}



  public static List<TotalReturnsDto> allquotes(List<PortfolioTrade> trades, String[] args, ObjectMapper objectMapper)
  throws JsonMappingException, JsonProcessingException {
List<TotalReturnsDto> st = new ArrayList<TotalReturnsDto>();
String token = "bf9fc8bfe29b6280578e504ee5a7c951488b88b0";
RestTemplate rest = new RestTemplate();
for (PortfolioTrade t : trades) {

  String url = prepareUrl(t, LocalDate.parse(args[1]), token);
  String result = (rest.getForObject(url, String.class));
  List<TiingoCandle> tc = objectMapper.readValue(result, new TypeReference<List<TiingoCandle>>(){});
  

  TiingoCandle c = tc.get(tc.size() - 1);

  TotalReturnsDto returns = new TotalReturnsDto(t.getSymbol(), c.getClose());
  st.add(returns);
}
return st;
}

 

public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
List<PortfolioTrade> trades = readTradesFromJson(args[0]);

ObjectMapper objectMapper = getObjectMapper();

List<TotalReturnsDto> stocks = allquotes(trades, args, objectMapper);

Collections.sort(stocks, new sortByClosingPrice());
List<String> toReturn = new ArrayList<String>();
for (TotalReturnsDto s : stocks) {
  toReturn.add(s.getSymbol());
}
return toReturn;
}

public static List<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {
  ObjectMapper om = getObjectMapper();
  File file = resolveFileFromResources(filename);
  List<PortfolioTrade> trade = om.readValue(file, new TypeReference<List<PortfolioTrade>>() {
  });
  return trade;
}


  
}

class sortByClosingPrice implements Comparator<TotalReturnsDto> {

  @Override
  public int compare(TotalReturnsDto arg0, TotalReturnsDto arg1) {
    // TODO Auto-generated method stub
    if (arg0.getClosingPrice() > arg1.getClosingPrice()) {
      return 1;
    }
    if (arg0.getClosingPrice() < arg1.getClosingPrice()) {
      return -1;
    }
    if (arg0.getClosingPrice() == arg1.getClosingPrice()) {
      return 0;
    }
    return 0;
  }

}


