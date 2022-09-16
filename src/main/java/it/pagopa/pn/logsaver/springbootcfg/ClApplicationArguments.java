package it.pagopa.pn.logsaver.springbootcfg;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.Data;


@Configuration
@Data
public class ClApplicationArguments {

  private LocalDate dateFrom;

  private LocalDate dateTo;

  private List<LocalDate> dateList;

  private List<ItemType> types;


  @Autowired
  void initDateFrom(@Value("${dateFrom:}") String dateFromStr) {
    this.dateFrom = DateUtils.parse(dateFromStr);
  }

  @Autowired
  void initDateTo(@Value("${dateTo:}") String dateFromStr) {
    this.dateTo = DateUtils.parse(dateFromStr);
  }

  @Autowired
  void initDateList(
      @Value("${dateList:}#{T(java.util.Collections).emptyList()}") List<String> dateListStr) {
    this.dateList = dateListStr.stream().map(DateUtils::parse).collect(Collectors.toList());
  }
}
