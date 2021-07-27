package com.bogdwellers.pinchtozoom;

import ohos.agp.components.Component;
import ohos.agp.components.Component.TouchEventListener;
import ohos.agp.utils.Point;
import ohos.multimodalinput.event.ManipulationEvent;
import ohos.multimodalinput.event.MmiPoint;
import ohos.multimodalinput.event.TouchEvent;
import ohos.utils.PlainArray;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>This class enables easy interpretation of multitouch gestures such as pinching, rotating etc.</p>
 *
 * @author Martin
 *
 */
public class MultiTouchListener implements TouchEventListener {
	

	
	/*
	 * Attributes
	 */
	
	private List<Integer> pointerIds;
	private PlainArray<Point> startPoints;
	
	/*
	 * Constructor(s)
	 */
	
	public MultiTouchListener() {
		this.pointerIds = new ArrayList<>(40); // 4 persons with both hands compatible :)
		this.startPoints = new PlainArray<>();
	}

	@Override
	public boolean onTouchEvent(Component component, TouchEvent touchEvent) {
		int actionMasked = touchEvent.getAction();
		int actionIndex = touchEvent.getIndex();
		Integer pointerId;

		// Handle touch event
		switch (actionMasked) {
			case TouchEvent.PRIMARY_POINT_DOWN:
			case TouchEvent.OTHER_POINT_DOWN:
				pointerId = touchEvent.getPointerId(actionIndex);
				Point startPoint = new Point(touchEvent.getPointerPosition(actionIndex).getX(),
						touchEvent.getPointerPosition(actionIndex).getY());
				// Save the starting point
				startPoints.put(pointerId, startPoint);
				pointerIds.add(pointerId);
				break;
			case TouchEvent.PRIMARY_POINT_UP:
			case TouchEvent.OTHER_POINT_UP:
				pointerId = touchEvent.getPointerId(actionIndex);
				pointerIds.remove(pointerId);
				startPoints.remove(pointerId);
				break;
			case TouchEvent.CANCEL:
				clearPointerIds();
				startPoints.clear();
				break;
			default:
				break;
		}
		return false;
	}

	/**
	 * <p>Clears all registered pointer ids.</p>
	 */
	private void clearPointerIds() {
		pointerIds.clear();
	}
	
	/**
	 * <p>Returns the current amount of touch points.</p>
	 * @return
	 */
	public int getTouchCount() {
		return pointerIds.size();
	}
	
	/**
	 * <p>Indicates if one or more touches are currently in progress.</p>
	 * @return
	 */
	public boolean isTouching() {
		return !pointerIds.isEmpty();
	}
	
	/**
	 * <p>Returns the pointer id for the given index of subsequent touch points.</p>
	 * @param touchNo
	 * @return
	 */
	public int getId(int touchNo) {
		return pointerIds.get(touchNo);
	}
	
	/**
	 * <p>Returns the start point for the given touch number (where the user initially pressed down).</p>
	 * @param touchNo
	 * @return
	 */
	public Point getStartPoint(int touchNo) { return startPoints.get(getId(touchNo)).get();
	}
	
	/**
	 * <p>Updates the start points with the current coordinate configuration.</p>
	 * @param event
	 */
	public void updateStartPoints(TouchEvent event) {
		Point startPoint;
		Integer pointerId;
		
		for(int i = 0, n = event.getPointerCount(); i < n; i++) {
			pointerId = event.getPointerId(i);
			MmiPoint point1 = event.getPointerPosition(pointerId);
			final float eventx = point1.getX();
			final float eventy = point1.getY();
			startPoint = new Point(eventx, eventy);
			
			// Save the starting point
			startPoints.put(pointerId, startPoint);
		}
	}
	
	/**
	 * <p>Returns an array containing all pointer ids.</p>
	 * @param ids
	 * @return
	 */
	public Integer[] getIdArray(Integer[] ids) {
		return pointerIds.toArray(ids);
	}
	
	/*
	 * Static methods
	 */
	
	/**
	 * 
	 * @param point
	 * @param event
	 * @param id
	 */
	public static final void point(Point point, TouchEvent event, int id) {
		int index = event.getPointerId(id);
		MmiPoint point1 = event.getPointerPosition(index);
		final float eventx = point1.getX();
		final float eventy = point1.getY();
		point.modify(eventx,eventy);
	}
	
	/**
	 * <p>Calculates the space between two pointers.</p>
	 * @param event
	 * @param pointerA id of pointer A
	 * @param pointerB id of pointer B
	 * @return spacing between both pointers
	 */
	public static final float spacing(TouchEvent event, int pointerA, int pointerB) {
		int indexA = event.getPointerId(pointerA);
		int indexB = event.getPointerId(pointerB);
		return spacingByIndex(event, indexA, indexB);
	}

	/**
	 *
	 * @param event
	 * @param indexA
	 * @param indexB
     * @return
     */
	private static final float spacingByIndex(ManipulationEvent event, int indexA, int indexB) {
		MmiPoint point1 = event.getPointerPosition(indexA);
		final float eventx1 = point1.getX();
		final float eventy1 = point1.getY();
		MmiPoint point2 = event.getPointerPosition(indexB);
		final float eventx2 = point2.getX();
		final float eventy2 = point2.getY();
		float x = eventx1 - eventx2;
		float y = eventy1 - eventy2;
		return (float) Math.sqrt(x * x + y * y); // Pythagoras
	}

	/**
	 * <p>Calculates the pinch velocity for the last <code>timeWindow</code> milliseconds.</p>
	 * @param event
	 * @param pointerA id of pointer A
	 * @param pointerB id of pointer B
	 * @param timeWindow
	 * @return spacing between both pointers
	 */
	public static final float pinchVelocity(ManipulationEvent event, int pointerA, int pointerB, long timeWindow) {
		int indexA = event.getPointerId(pointerA);
		int indexB = event.getPointerId(pointerB);
		long eventTime = event.getOccurredTime();
		long timeDelta = 0;
		float previousSpacing = spacingByIndex(event, indexA, indexB);
		float scale = 1;
		for(int i = 0, n = event.getPointerCount(); i < n && timeDelta < timeWindow; i++) {

			float x = event.getPointerPosition(indexA).getX() - event.getPointerPosition(indexB).getX();
			float y = event.getPointerPosition(indexA).getY() - event.getPointerPosition(indexB).getY();
			float spacing = (float) Math.sqrt(x * x + y * y);
			scale *= previousSpacing / spacing;
			previousSpacing = spacing;
			timeDelta = eventTime - event.getStartTime();
		}
		return (float) Math.pow(Math.pow(scale, 1d / timeWindow), 1000d);
	}

	/**
	 * <p>Calculates the mid point between two pointers.</p>
	 * @param point
	 * @param event
	 * @param pointerA id of pointer A
	 * @param pointerB id of pointer B
	 */
	public static final void midPoint(Point point, TouchEvent event, int pointerA, int pointerB) {
		int indexA = event.getPointerId(pointerA);
		int indexB = event.getPointerId(pointerB);
		MmiPoint point1 = event.getPointerPosition(indexA);
		final float eventx1 = point1.getX();
		final float eventy1 = point1.getY();
		MmiPoint point2 = event.getPointerPosition(indexB);
		final float eventx2 = point2.getX();
		final float eventy2 = point2.getY();
		float x = eventx1 + eventx2;
		float y = eventy1 + eventy2;
		point.equals(Math.round(x / 2f), Math.round(y / 2f));
	}
	
	/**
	 * <p>Calculates the angle between two points.</p>
	 * @param event
	 * @param pointerA id of pointer A
	 * @param pointerB id of pointer B
	 * @param isPointerAPivot indicates if pointer A is considered to be the pivot, else pointer B is. Use {@link #startedLower(Point, Point)}
	 * @return angle in degrees
	 */
	public static final float angle(TouchEvent event, int pointerA, int pointerB, boolean isPointerAPivot) {
		// Resolve the indices
		int indexA = event.getPointerId(pointerA);
		int indexB = event.getPointerId(pointerB);
		MmiPoint point1 = event.getPointerPosition(indexA);
		final float eventx1 = point1.getX();
		final float eventy1 = point1.getY();
		MmiPoint point2 = event.getPointerPosition(indexB);
		final float eventx2 = point2.getX();
		final float eventy2 = point2.getY();
		float x = eventx1 - eventx2;
		float y = eventy1 - eventy2;
		
		// Calculate the arc tangent
		double atan = Math.atan(x / y);
		
		// Always consider the same pointer the pivot
		if((y < 0f && isPointerAPivot) || (y > 0f && !isPointerAPivot)) {
			atan += Math.PI;
		}
		
		// Convert to float in degrees
		double deg = Math.toDegrees(atan);
		return (float) deg;
	}
	
	/**
	 * <p>Convenience method to determine whether starting point A has a lower y-axis value than starting point B.
	 * Useful in conjunction with {@link #angle(TouchEvent, int, int, boolean)}.</p>
	 * @param pointA
	 * @param pointB
	 * @return
	 */
	public static final boolean startedLower(Point pointA, Point pointB) {
		return pointA.getPointY() < pointB.getPointY();
	}

}