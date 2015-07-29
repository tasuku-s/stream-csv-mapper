package jp.co.freemind.csv.internal
import jp.co.freemind.csv.CsvFormatter
import jp.co.freemind.csv.Location
import jp.co.freemind.csv.data.Sample
import spock.lang.Shared
import spock.lang.Specification

import java.util.stream.Collectors
/**
 * Created by kakusuke on 15/07/24.
 */
class CsvParserTest extends Specification {
  @Shared
  def CsvParser<Sample> parser = new CsvParser<Sample>(CsvFormatter.builder(Sample).with(Sample.CsvFormat).build())

  def "test formatter"() {
    when:
    def sniffer = new CsvErrorSniffer()
    def parsed = parser.parse(new ByteArrayInputStream('a,1,true'.getBytes("MS932")), sniffer).collect(Collectors.toList())

    then:
    assert !sniffer.hasError()
    assert parsed == [new Sample(a: "a", b: true, c: 1)]
  }

  def "test one error for each lines"() {
    when:
    def sniffer = new CsvErrorSniffer()
    def stream = parser.parse(new ByteArrayInputStream('a,a,true\nb,1,b'.getBytes("MS932")), sniffer)
    def parsed = stream.collect(Collectors.toList())
    stream.close()

    then:
    assert sniffer.locations == [new Location(1, OptionalInt.of(2)), new Location(2, OptionalInt.of(3))] as Set
    assert sniffer.hasError()
    assert parsed == [new Sample(a: "a", b: true, c: null), new Sample(a: "b", b: null, c: 1)]
  }

  def "test two error on one line"() {
    when:
    def sniffer = new CsvErrorSniffer()
    def stream = parser.parse(new ByteArrayInputStream('a,a,a'.getBytes("MS932")), sniffer)
    def parsed = stream.collect(Collectors.toList())
    stream.close()

    then:
    assert sniffer.locations == [new Location(1, OptionalInt.of(2)), new Location(1, OptionalInt.of(3))] as Set
    assert sniffer.hasError()
    assert parsed == [new Sample(a: "a", b: null, c: null)]
  }

  def "test with headers"() {
    given:
    def CsvParser<Sample> parser = new CsvParser<Sample>(CsvFormatter.builder(Sample).with(Sample.CsvFormat).withHeaders().build())

    when:
    def sniffer = new CsvErrorSniffer()
    def parsed = parser.parse(new ByteArrayInputStream('foo,bar,buz\r\na,a,a'.getBytes("MS932")), sniffer).collect(Collectors.toList())

    then:
    assert sniffer.locations == [new Location(2, OptionalInt.of(2)), new Location(2, OptionalInt.of(3))] as Set
    assert sniffer.hasError()
    assert parsed == [new Sample(a: "a", b: null, c: null)]
  }

  def "test with nullValue"() {
    given:
    def CsvParser<Sample> parser = new CsvParser<Sample>(CsvFormatter.builder(Sample).with(Sample.CsvFormat).nullValue('NULL').build())

    when:
    def sniffer = new CsvErrorSniffer()
    def parsed = parser.parse(new ByteArrayInputStream('NULL,NULL,NULL'.getBytes("MS932")), sniffer).collect(Collectors.toList())

    then:
    assert parsed == [new Sample(a: null, b: null, c: null)]
    assert !sniffer.hasError()
  }

}