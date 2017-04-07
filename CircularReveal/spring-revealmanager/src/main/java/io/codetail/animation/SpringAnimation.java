/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codetail.animation;

import android.os.Looper;
import android.util.AndroidRuntimeException;

/**
 * SpringAnimation is an animation that is driven by a {@link SpringForce}. The spring force defines
 * the spring's stiffness, damping ratio, as well as the rest position. Once the SpringAnimation is
 * started, on each frame the spring force will update the animation's value and velocity.
 * The animation will continue to run until the spring force reaches equilibrium. If the spring used
 * in the animation is undamped, the animation will never reach equilibrium. Instead, it will
 * oscillate forever.
 */
final class SpringAnimation extends DynamicAnimation<SpringAnimation> {

  private SpringForce mSpring = null;
  private float mPendingPosition = UNSET;
  private static final float UNSET = Float.MAX_VALUE;

  /**
   * This creates a SpringAnimation that animates the property of the given view.
   * Note, a spring will need to setup through {@link #setSpring(SpringForce)} before
   * the animation starts.
   *
   * @param v The View whose property will be animated
   * @param property the property index of the view
   */
  public <T> SpringAnimation(T v, Property<T> property) {
    super(v, property);
  }

  /**
   * This creates a SpringAnimation that animates the property of the given view. A Spring will be
   * created with the given final position and default stiffness and damping ratio.
   * This spring can be accessed and reconfigured through {@link #setSpring(SpringForce)}.
   *
   * @param v The View whose property will be animated
   * @param property the property index of the view
   * @param finalPosition the final position of the spring to be created.
   */
  public <T> SpringAnimation(T v, Property<T> property, float finalPosition) {
    super(v, property);
    mSpring = new SpringForce(finalPosition);
    setSpringThreshold();
  }

  /**
   * Returns the spring that the animation uses for animations.
   *
   * @return the spring that the animation uses for animations
   */
  public SpringForce getSpring() {
    return mSpring;
  }

  /**
   * Uses the given spring as the force that drives this animation. If this spring force has its
   * parameters re-configured during the animation, the new configuration will be reflected in the
   * animation immediately.
   *
   * @param force a pre-defined spring force that drives the animation
   * @return the animation that the spring force is set on
   */
  public SpringAnimation setSpring(SpringForce force) {
    mSpring = force;
    setSpringThreshold();
    return this;
  }

  @Override
  public void start() {
    sanityCheck();
    super.start();
  }

  /**
   * Updates the final position of the spring.
   * <p/>
   * When the animation is running, calling this method would assume the position change of the
   * spring as a continuous movement since last frame, which yields more accurate results than
   * changing the spring position directly through {@link SpringForce#setFinalPosition(float)}.
   * <p/>
   * If the animation hasn't started, calling this method will change the spring position, and
   * immediately start the animation.
   *
   * @param finalPosition rest position of the spring
   */
  public void animateToFinalPosition(float finalPosition) {
    if (isRunning()) {
      mPendingPosition = finalPosition;
    } else {
      if (mSpring == null) {
        mSpring = new SpringForce(finalPosition);
      }
      mSpring.setFinalPosition(finalPosition);
      start();
    }
  }

  /**
   * Skips to the end of the animation. If the spring is undamped, an
   * {@link IllegalStateException} will be thrown, as the animation would never reach to an end.
   * It is recommended to check {@link #canSkipToEnd()} before calling this method. This method
   * should only be called on main thread. If animation is not running, no-op.
   *
   * @throws IllegalStateException if the spring is undamped (i.e. damping ratio = 0)
   * @throws AndroidRuntimeException if this method is not called on the main thread
   */
  public void skipToEnd() {
    if (!canSkipToEnd()) {
      throw new UnsupportedOperationException("Spring animations can only come to an end"
          + " when there is damping");
    }
    if (Looper.myLooper() != Looper.getMainLooper()) {
      throw new AndroidRuntimeException("Animations may only be started on the main thread");
    }
    if (mRunning) {
      if (mPendingPosition != UNSET) {
        mSpring.setFinalPosition(mPendingPosition);
        mPendingPosition = UNSET;
      }
      mValue = mSpring.getFinalPosition();
      mVelocity = 0;
      cancel();
    }
  }

  /**
   * Queries whether the spring can eventually come to the rest position.
   *
   * @return {@code true} if the spring is damped, otherwise {@code false}
   */
  public boolean canSkipToEnd() {
    return mSpring.mDampingRatio > 0;
  }

  /************************ Below are private APIs *************************/

  private void setSpringThreshold() {
    if (mViewProperty == ROTATION || mViewProperty == ROTATION_X
        || mViewProperty == ROTATION_Y) {
      mSpring.setDefaultThreshold(SpringForce.VALUE_THRESHOLD_ROTATION);
    } else if (mViewProperty == ALPHA) {
      mSpring.setDefaultThreshold(SpringForce.VALUE_THRESHOLD_ALPHA);
    } else if (mViewProperty == SCALE_X || mViewProperty == SCALE_Y) {
      mSpring.setDefaultThreshold(SpringForce.VALUE_THRESHOLD_SCALE);
    } else {
      mSpring.setDefaultThreshold(SpringForce.VALUE_THRESHOLD_IN_PIXEL);
    }
  }

  private void sanityCheck() {
    if (mSpring == null) {
      throw new UnsupportedOperationException("Incomplete SpringAnimation: Either final"
          + " position or a spring force needs to be set.");
    }
    double finalPosition = mSpring.getFinalPosition();
    if (finalPosition > mMaxValue) {
      throw new UnsupportedOperationException("Final position of the spring cannot be greater"
          + " than the max value.");
    } else if (finalPosition < mMinValue) {
      throw new UnsupportedOperationException("Final position of the spring cannot be less"
          + " than the min value.");
    }
  }

  @Override
  boolean updateValueAndVelocity(long deltaT) {
    if (mPendingPosition != UNSET) {
      double lastPosition = mSpring.getFinalPosition();
      // Approximate by considering half of the time spring position stayed at the old
      // position, half of the time it's at the new position.
      SpringForce.MassState massState = mSpring.updateValues(mValue, mVelocity, deltaT / 2);
      mSpring.setFinalPosition(mPendingPosition);
      mPendingPosition = UNSET;

      massState = mSpring.updateValues(massState.mValue, massState.mVelocity, deltaT / 2);
      mValue = massState.mValue;
      mVelocity = massState.mVelocity;
    } else {
      SpringForce.MassState massState = mSpring.updateValues(mValue, mVelocity, deltaT);
      mValue = massState.mValue;
      mVelocity = massState.mVelocity;
    }

    mValue = Math.max(mValue, mMinValue);
    mValue = Math.min(mValue, mMaxValue);

    if (isAtEquilibrium(mValue, mVelocity)) {
      mValue = mSpring.getFinalPosition();
      mVelocity = 0f;
      return true;
    }
    return false;
  }

  @Override
  float getAcceleration(float value, float velocity) {
    return mSpring.getAcceleration(value, velocity);
  }

  @Override
  boolean isAtEquilibrium(float value, float velocity) {
    return mSpring.isAtEquilibrium(value, velocity);
  }
}
