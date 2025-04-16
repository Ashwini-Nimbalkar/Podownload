package com.tru.podownload;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.apache.camel.routepolicy.quartz.CronScheduledRoutePolicy;
import org.apache.camel.spi.PropertiesComponent;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import java.sql.SQLNonTransientConnectionException;
import org.apache.camel.LoggingLevel;

public class SqlRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties");
		// ENV.load(in);

		Properties props = new Properties();

		props.load(in);
		PropertiesComponent prc = getContext().getPropertiesComponent();
		prc.setInitialProperties(props);
		getContext().setPropertiesComponent(prc);

		PropertiesComponent prc2 = getContext().getPropertiesComponent();

		String Url = prc2.loadProperties().getProperty("Url");
		String DriverClassName = prc2.loadProperties().getProperty("DriverClassName");
		String Username = prc2.loadProperties().getProperty("Username");
		String Password = prc2.loadProperties().getProperty("Password");

		DataSource dataSource = setupDataSource(Url, DriverClassName, Username, Password);

		// SimpleRegistry reg = new SimpleRegistry() ;
		// reg.bind("myds",dataSource);

		getContext().getRegistry().bind("mydata", dataSource);

		CsvDataFormat csv = new CsvDataFormat();
		csv.setQuoteDisabled(true);
		CronScheduledRoutePolicy startPolicy = new CronScheduledRoutePolicy();
		startPolicy.setRouteStartTime("* 01 17 * * ?");
		// startPolicy.setRouteSuspendTime("* 52 15 * * ?");
		startPolicy.setRouteStopTime("0 05 17 * * ?");

		onException(SQLNonTransientConnectionException.class).continued(true)
				.log(LoggingLevel.ERROR, "An Error occured!").log("Tried Again").maximumRedeliveries(5);
		from("{{sql.timer}}").routeId("PODownloadSql-Route").routePolicy(startPolicy).noAutoStartup()
		.to("{{seq.query}}")
		.setHeader("true", simple("true"))
		.split(body())
		.setHeader("seq", simple("${body[FEEDSEQ]}"))
		/*.to("{{cinpodownload.query}}")
		 .marshal(csv)
	    .toD("{{cinpodownloadsqlfiles.path}}")
	     .end();*/
		 .to("{{cinpodownload.query}}").marshal(csv).toD("file:C://in/CIN?fileName=CIN_PO_${date:now:yyyyMMdd}_${header.seq}.txt").end();

	}

	private DataSource setupDataSource(String Url, String DriverClassName, String Username, String Password)
			throws SQLException {
		BasicDataSource ds = new BasicDataSource();
		ds.setDriverClassName(DriverClassName);
		ds.setUsername(Username);
		ds.setPassword(Password);
		ds.setUrl(Url);
		return ds;
	}

}
