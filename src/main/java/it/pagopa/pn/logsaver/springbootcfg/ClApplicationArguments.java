package it.pagopa.pn.logsaver.springbootcfg;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
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

  @Autowired
  void initTypeList(
      @Value("${types:}#{T(java.util.Collections).emptyList()}") List<String> typeListStr) {
    this.types = typeListStr.stream().map(ItemType::valueOf).collect(Collectors.toList());
  }

  @PostConstruct
  void validate() {
    if (Objects.nonNull(dateFrom) && Objects.nonNull(dateTo)) {
      if (dateFrom.isAfter(dateTo) || dateTo.isAfter(DateUtils.yesterday())) {
        throw new IllegalArgumentException(
            "Argument dateFrom must be less than or equal to date. Argument dateTo must be less than yesterday's date.");
      }
    }
  }
}
