package us.ihmc.robotics.screwTheory;

import org.ejml.data.DenseMatrix64F;

public interface MassMatrixCalculator
{

   public abstract void compute();

   public abstract DenseMatrix64F getMassMatrix();

   public abstract InverseDynamicsJoint[] getJointsInOrder();
}