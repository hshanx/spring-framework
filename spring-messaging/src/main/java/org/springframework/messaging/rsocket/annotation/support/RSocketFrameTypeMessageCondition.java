/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.messaging.rsocket.annotation.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.rsocket.frame.FrameType;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.AbstractMessageCondition;
import org.springframework.util.Assert;

/**
 * A condition to assist with mapping onto handler methods based on the RSocket
 * frame type. This helps to separate the handling of connection-level frame
 * types, i.e. {@code SETUP} and {@code METADATA_PUSH}, from the handling of
 * stream requests.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RSocketFrameTypeMessageCondition extends AbstractMessageCondition<RSocketFrameTypeMessageCondition> {

	/** The name of the header that contains the RSocket frame type being processed. */
	public static final String FRAME_TYPE_HEADER = "rsocketFrameType";


	/** Per FrameType cache to return ready instances from getMatchingCondition. */
	private static final Map<String, RSocketFrameTypeMessageCondition> frameTypeConditionCache;

	static {
		frameTypeConditionCache = new HashMap<>(FrameType.values().length);
		for (FrameType type : FrameType.values()) {
			frameTypeConditionCache.put(type.name(), new RSocketFrameTypeMessageCondition(type));
		}
	}


	static final RSocketFrameTypeMessageCondition CONNECT_CONDITION =
			new RSocketFrameTypeMessageCondition(FrameType.SETUP, FrameType.METADATA_PUSH);

	static final RSocketFrameTypeMessageCondition EMPTY_CONDITION = new RSocketFrameTypeMessageCondition();



	private final Set<FrameType> frameTypes;


	public RSocketFrameTypeMessageCondition(FrameType... frameType) {
		this(Arrays.asList(frameType));
	}

	public RSocketFrameTypeMessageCondition(Collection<FrameType> frameTypes) {
		Assert.notEmpty(frameTypes, "`frameTypes` are required");
		this.frameTypes = Collections.unmodifiableSet(new LinkedHashSet<>(frameTypes));
	}

	private RSocketFrameTypeMessageCondition() {
		this.frameTypes = Collections.emptySet();
	}


	public Set<FrameType> getFrameTypes() {
		return this.frameTypes;
	}

	@Override
	protected Collection<?> getContent() {
		return this.frameTypes;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * Find the RSocket frame type in the message headers.
	 * @param message the current message
	 * @return the frame type or {@code null} if not found
	 */
	@SuppressWarnings("ConstantConditions")
	@Nullable
	public static FrameType getFrameType(Message<?> message) {
		return (FrameType) message.getHeaders().get(RSocketFrameTypeMessageCondition.FRAME_TYPE_HEADER);
	}


	@Override
	public RSocketFrameTypeMessageCondition combine(RSocketFrameTypeMessageCondition other) {
		if (this.frameTypes.equals(other.frameTypes)) {
			return other;
		}
		Set<FrameType> set = new LinkedHashSet<>(this.frameTypes);
		set.addAll(other.frameTypes);
		return new RSocketFrameTypeMessageCondition(set);
	}

	@Override
	public RSocketFrameTypeMessageCondition getMatchingCondition(Message<?> message) {
		FrameType actual = message.getHeaders().get(FRAME_TYPE_HEADER, FrameType.class);
		if (actual != null) {
			for (FrameType type : this.frameTypes) {
				if (actual == type) {
					return frameTypeConditionCache.get(type.name());
				}
			}
		}
		return  null;
	}

	@Override
	public int compareTo(RSocketFrameTypeMessageCondition other, Message<?> message) {
		return other.frameTypes.size() - this.frameTypes.size();
	}


	/**
	 * Return a condition for matching the RSocket request interaction type with
	 * that is selected based on the delcared request and response cardinality
	 * of some handler method.
	 * <p>The table below shows the selections made:
	 * <table>
	 * <tr>
	 * <th>Request Cardinality</th>
	 * <th>Response Cardinality</th>
	 * <th>Interaction Types</th>
	 * </tr>
	 * <tr>
	 * <td>0,1</td>
	 * <td>0</td>
	 * <td>Fire-And-Forget, Request-Response</td>
	 * </tr>
	 * <tr>
	 * <td>0,1</td>
	 * <td>1</td>
	 * <td>Request-Response</td>
	 * </tr>
	 * <tr>
	 * <td>0,1</td>
	 * <td>2</td>
	 * <td>Request-Stream</td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td>Any</td>
	 * <td>Request-Channel</td>
	 * </tr>
	 * </table>
	 * @param cardinalityIn -- the request cardinality: 1 for a single payload,
	 * 2 for many payloads, and 0 if input is not handled.
	 * @param cardinalityOut -- the response cardinality: 0 for no output
	 * payloads, 1 for a single payload, and 2 for many payloads.
	 * @return a condition to use for matching the interaction type
	 * @since 5.2.2
	 */
	public static RSocketFrameTypeMessageCondition getCondition(int cardinalityIn, int cardinalityOut) {
		switch (cardinalityIn) {
			case 0:
			case 1:
				switch (cardinalityOut) {
					case 0: return FF_RR_CONDITION;
					case 1: return RR_CONDITION;
					case 2: return RS_CONDITION;
					default: throw new IllegalStateException("Invalid cardinality: " + cardinalityOut);
				}
			case 2:
				return RC_CONDITION;
			default:
				throw new IllegalStateException("Invalid cardinality: " + cardinalityIn);
		}
	}


	private static final RSocketFrameTypeMessageCondition FF_CONDITION = from(FrameType.REQUEST_FNF);
	private static final RSocketFrameTypeMessageCondition RR_CONDITION = from(FrameType.REQUEST_RESPONSE);
	private static final RSocketFrameTypeMessageCondition RS_CONDITION = from(FrameType.REQUEST_STREAM);
	private static final RSocketFrameTypeMessageCondition RC_CONDITION = from(FrameType.REQUEST_CHANNEL);
	private static final RSocketFrameTypeMessageCondition FF_RR_CONDITION = FF_CONDITION.combine(RR_CONDITION);

	private static RSocketFrameTypeMessageCondition from(FrameType... frameTypes) {
		return new RSocketFrameTypeMessageCondition(frameTypes);
	}

}
