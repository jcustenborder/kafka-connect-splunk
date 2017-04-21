package com.github.jcustenborder.kafka.connect.splunk;

import com.github.jcustenborder.kafka.connect.utils.BaseDocumentationTest;
import org.apache.kafka.connect.data.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class DocumentationTest extends BaseDocumentationTest {
  private static final Logger log = LoggerFactory.getLogger(DocumentationTest.class);

  @Override
  protected String[] packages() {
    return new String[]{this.getClass().getPackage().getName()};
  }


  @Override
  protected List<Schema> schemas() {
    return Arrays.asList(
        EventConverter.KEY_SCHEMA,
        EventConverter.VALUE_SCHEMA
    );
  }

}
