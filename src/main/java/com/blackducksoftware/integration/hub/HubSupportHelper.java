/*******************************************************************************
 * Black Duck Software Suite SDK
 * Copyright (C) 2016 Black Duck Software, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *******************************************************************************/
package com.blackducksoftware.integration.hub;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.restlet.resource.ResourceException;

import com.blackducksoftware.integration.hub.api.VersionComparison;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.logging.IntLogger;

public class HubSupportHelper implements Serializable {
	private static final long serialVersionUID = 6440466357358359056L;

	private boolean hasBeenChecked = false;
	private boolean hub3_0Support = false;

	/**
	 * CLI wrappers were packaged with OS specific Jre's since Hub 3.0.0
	 */
	public boolean isJreProvidedSupport() {
		return hub3_0Support;
	}

	/**
	 * Policy Api's were added since Hub 3.0.0
	 */
	public boolean isPolicyApiSupport() {
		return hub3_0Support;
	}

	/**
	 * The CLI started supporting an option to print status files to a directory
	 * since Hub 3.0.0
	 */
	public boolean isCliStatusDirOptionSupport() {
		return hub3_0Support;
	}

	private void setHub3_0Support(final boolean hub3_0Support) {
		this.hub3_0Support = hub3_0Support;
	}

	public boolean isHasBeenChecked() {
		return hasBeenChecked;
	}

	public void setHasBeenChecked(final boolean hasBeenChecked) {
		this.hasBeenChecked = hasBeenChecked;
	}

	/**
	 * This will check the Hub server to see which options this version of the
	 * Hub supports. You can use the get methods in this class after this method
	 * has run to get the supported options.
	 */
	public void checkHubSupport(final HubIntRestService service, final IntLogger logger)
			throws IOException, URISyntaxException {
		try {
			final String hubServerVersion = service.getHubVersion();

			if (compareVersion(hubServerVersion, "3.0.0", service)) {
				setHub3_0Support(true);
			} else {
				setHub3_0Support(false);
			}
			setHasBeenChecked(true);
		} catch (final BDRestException e) {
			ResourceException resEx = null;
			if (e.getCause() != null && e.getCause() instanceof ResourceException) {
				resEx = (ResourceException) e.getCause();
			}
			if (resEx != null) {
				if (logger != null) {
					logger.error(resEx.getMessage());
				}
			}
			if (logger != null) {
				logger.error(e.getMessage());
			}
		}
	}

	/**
	 * This method will check the provided version against the actual version of
	 * the Hub server. If the provided version is less than or equal to the
	 * server version we return true. If the provided version is greater than
	 * the server version we return false.
	 */
	private boolean compareVersion(final String hubServerVersion, final String testVersion,
			final HubIntRestService service) throws IOException, BDRestException, URISyntaxException {
		try {
			final String[] splitServerVersion = hubServerVersion.split("\\.");
			final String[] splitTestVersion = testVersion.split("\\.");

			final Integer[] serverVersionParts = new Integer[splitServerVersion.length];
			final Integer[] testVersionParts = new Integer[splitTestVersion.length];
			boolean isServerSnapshot = false;
			for (int i = 0; i < splitServerVersion.length; i++) {
				String currentServerPart = splitServerVersion[i];
				final String currentTestVersionPart = splitTestVersion[i];
				if (currentServerPart.contains("-SNAPSHOT")) {
					isServerSnapshot = true;
					currentServerPart = currentServerPart.replace("-SNAPSHOT", "");
				}
				serverVersionParts[i] = Integer.valueOf(currentServerPart);
				testVersionParts[i] = Integer.valueOf(currentTestVersionPart);
			}

			if (serverVersionParts[0] > testVersionParts[0]) {
				// Major part of the server version was greater,
				// so we know it supports whatever feature we are testing for
				return true;
			} else if (serverVersionParts[0] < testVersionParts[0]) {
				// Major part of the server version was less than the one
				// provided,
				// so we know it does not support whatever feature we are
				// testing for
				return false;
			}

			if (serverVersionParts[1] > testVersionParts[1]) {
				// Minor part of the server version was greater,
				// so we know it supports whatever feature we are testing for
				return true;
			} else if (serverVersionParts[1] < testVersionParts[1]) {
				// Minor part of the server version was less than the one
				// provided,
				// so we know it does not support whatever feature we are
				// testing for
				return false;
			}

			if (serverVersionParts[2] > testVersionParts[2]) {
				// Fix version part of the server version was greater,
				// so we know it supports whatever feature we are testing for
				return true;
			} else if (serverVersionParts[2] < testVersionParts[2]) {
				// Fix version part of the server version was less than the one
				// provided,
				// so we know it does not support whatever feature we are
				// testing for
				return false;
			}

			// The versions are identical, check if the server is a SNAPSHOT
			if (isServerSnapshot) {
				// We assume the SNAPSHOT version is less than the released
				// version
				return false;
			}
		} catch (final NumberFormatException e) {
			return fallBackComparison(testVersion, service);
		} catch (final ArrayIndexOutOfBoundsException e) {
			return fallBackComparison(testVersion, service);
		}

		return true;
	}

	/**
	 * We are parsing the versions incorrectly so we let the Hub server compare
	 * the test version to the server version. We return true if the testVersion
	 * is less than or equal to the server version We return false if the
	 * testVersion is greater than the server version.
	 *
	 */
	private boolean fallBackComparison(final String testVersion, final HubIntRestService service)
			throws IOException, BDRestException, URISyntaxException {
		final VersionComparison comparison = service.compareWithHubVersion(testVersion);
		if (comparison.getNumericResult() <= 0) {
			return true;
		} else {
			return false;
		}
	}

	private static String getCLIWrapperLink(final String hubUrl) throws IllegalArgumentException {
		if (StringUtils.isBlank(hubUrl)) {
			throw new IllegalArgumentException("You must provide a valid Hub URL in order to get the correct link.");
		}
		final StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(hubUrl);
		if (!hubUrl.endsWith("/")) {
			urlBuilder.append("/");
		}
		urlBuilder.append("download");
		urlBuilder.append("/");
		return urlBuilder.toString();
	}

	public static String getLinuxCLIWrapperLink(final String hubUrl) throws IllegalArgumentException {
		final String baseUrl = getCLIWrapperLink(hubUrl);
		final StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(baseUrl);
		urlBuilder.append("scan.cli.zip");
		return urlBuilder.toString();
	}

	public static String getWindowsCLIWrapperLink(final String hubUrl) throws IllegalArgumentException {
		final String baseUrl = getCLIWrapperLink(hubUrl);
		final StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(baseUrl);
		urlBuilder.append("scan.cli-windows.zip");
		return urlBuilder.toString();
	}

	public static String getOSXCLIWrapperLink(final String hubUrl) throws IllegalArgumentException {
		final String baseUrl = getCLIWrapperLink(hubUrl);
		final StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(baseUrl);
		urlBuilder.append("scan.cli-macosx.zip");
		return urlBuilder.toString();
	}

}
