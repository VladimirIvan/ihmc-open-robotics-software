package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import static us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointTools.computeDesiredCapturePointPosition;
import static us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointTools.computeDesiredCapturePointVelocity;
import static us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointTools.computeDesiredCornerPoints;

import java.util.ArrayList;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.configurations.CapturePointPlannerParameters;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointTools;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.dataStructures.variable.IntegerYoVariable;
import us.ihmc.robotics.geometry.FrameLine2d;
import us.ihmc.robotics.geometry.FrameLineSegment2d;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFramePoint2d;
import us.ihmc.robotics.math.frames.YoFramePointInMultipleFrames;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.math.frames.YoFrameVector2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition.GraphicType;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsList;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.plotting.ArtifactList;

public class ICPPlanner
{
   private static final boolean VISUALIZE = false;
   private static final double ICP_CORNER_POINT_SIZE = 0.004;

   protected final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final String namePrefix = "icpPlanner";

   private final BooleanYoVariable isStanding = new BooleanYoVariable(namePrefix + "IsStanding", registry);
   private final BooleanYoVariable isInitialTransfer = new BooleanYoVariable(namePrefix + "IsInitialTransfer", registry);
   private final BooleanYoVariable isDoubleSupport = new BooleanYoVariable(namePrefix + "IsDoubleSupport", registry);
   private final DoubleYoVariable timeInCurrentState = new DoubleYoVariable(namePrefix + "TimeInCurrentState", registry);
   private final DoubleYoVariable doubleSupportDuration = new DoubleYoVariable(namePrefix + "DoubleSupportTime", registry);
   private final DoubleYoVariable singleSupportDuration = new DoubleYoVariable(namePrefix + "SingleSupportTime", registry);
   private final DoubleYoVariable minSingleSupportDuration = new DoubleYoVariable(namePrefix + "MinSingleSupportTime", registry);
   private final DoubleYoVariable doubleSupportInitialTransferDuration = new DoubleYoVariable(namePrefix + "InitialTransferDuration", registry);
   private final DoubleYoVariable doubleSupportSplitFraction = new DoubleYoVariable(namePrefix + "DoubleSupportSplitFraction", registry);
   private final DoubleYoVariable initialTime = new DoubleYoVariable(namePrefix + "InitialTime", registry);
   private final DoubleYoVariable remainingTime = new DoubleYoVariable(namePrefix + "RemainingTime", registry);
   private final IntegerYoVariable numberFootstepsToConsider = new IntegerYoVariable(namePrefix + "NumberFootstepsToConsider", registry);

   private final DoubleYoVariable velocityDecayDurationWhenDone = new DoubleYoVariable(namePrefix + "VelocityDecayDurationWhenDone", registry);
   private final DoubleYoVariable velocityReductionFactor = new DoubleYoVariable(namePrefix + "VelocityReductionFactor", registry);

   private final YoFramePointInMultipleFrames singleSupportInitialICP;
   private final YoFrameVector singleSupportInitialICPVelocity = new YoFrameVector(namePrefix + "SingleSupportInitialICPVelocity", worldFrame, registry);

   private final YoFramePointInMultipleFrames singleSupportFinalICP;
   private final YoFrameVector singleSupportFinalICPVelocity = new YoFrameVector(namePrefix + "SingleSupportFinalICPVelocity", worldFrame, registry);

   private final YoFramePoint desiredCentroidalMomentumPivotPosition = new YoFramePoint(namePrefix + "DesiredCentroidalMomentumPosition", worldFrame, registry);
   private final YoFrameVector desiredCentroidalMomentumPivotVelocity = new YoFrameVector(namePrefix + "DesiredCentroidalMomentumVelocity", worldFrame, registry);
   private final YoFramePoint desiredCapturePointPosition = new YoFramePoint(namePrefix + "DesiredCapturePointPosition", worldFrame, registry);
   private final YoFrameVector desiredCapturePointVelocity = new YoFrameVector(namePrefix + "DesiredCapturePointVelocity", worldFrame, registry);
   private final YoFrameVector desiredCapturePointAcceleration = new YoFrameVector(namePrefix + "DesiredCapturePointAcceleration", worldFrame, registry);
   private final DoubleYoVariable omega0 = new DoubleYoVariable(namePrefix + "Omega0", registry);

   private final BooleanYoVariable requestedHoldPosition = new BooleanYoVariable(namePrefix + "RequestedHoldPosition", registry);
   private final BooleanYoVariable isHoldingPosition = new BooleanYoVariable(namePrefix + "IsHoldingPosition", registry);
   private final YoFramePoint actualICPToHold = new YoFramePoint(namePrefix + "ActualCapturePointToHold", worldFrame, registry);

   private final BooleanYoVariable useTwoConstantCMPsPerSupport = new BooleanYoVariable(namePrefix + "UseTwoConstantCMPsPerSupport", registry);
   private final DoubleYoVariable exitCMPDurationInPercentOfStepTime = new DoubleYoVariable(namePrefix + "TimeSpentOnExitCMPInPercentOfStepTime", registry);

   private final EnumYoVariable<RobotSide> transferToSide = new EnumYoVariable<>(namePrefix + "TransferToSide", registry, RobotSide.class, true);
   private final EnumYoVariable<RobotSide> supportSide = new EnumYoVariable<>(namePrefix + "SupportSide", registry, RobotSide.class, true);

   private final ArrayList<YoFramePointInMultipleFrames> entryCornerPoints = new ArrayList<YoFramePointInMultipleFrames>();
   private final ArrayList<YoFramePointInMultipleFrames> exitCornerPoints = new ArrayList<YoFramePointInMultipleFrames>();

   private final ICPPlannerTrajectoryGenerator icpDoubleSupportTrajectoryGenerator;
   private final ICPPlannerSegmentedTrajectoryGenerator icpSingleSupportTrajectoryGenerator;
   private final ReferenceCentroidalMomentumPivotLocationsCalculator referenceCMPsCalculator;

   private final ReferenceFrame midFeetZUpFrame;
   private final SideDependentList<ReferenceFrame> soleZUpFrames;

   private final FramePoint tempConstantCMP = new FramePoint();
   private final FramePoint tempICP = new FramePoint();

   public ICPPlanner(BipedSupportPolygons bipedSupportPolygons, SideDependentList<? extends ContactablePlaneBody> contactableFeet,
         CapturePointPlannerParameters icpPlannerParameters, YoVariableRegistry parentRegistry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      isStanding.set(true);

      actualICPToHold.setToNaN();
      isHoldingPosition.set(false);

      icpDoubleSupportTrajectoryGenerator = new ICPPlannerTrajectoryGenerator(namePrefix + "DoubleSupport", worldFrame, registry);
      icpSingleSupportTrajectoryGenerator = new ICPPlannerSegmentedTrajectoryGenerator(namePrefix + "SingleSupport", worldFrame, omega0, yoGraphicsListRegistry,
            registry);
      icpSingleSupportTrajectoryGenerator.setMaximumSplineDuration(icpPlannerParameters.getMaxDurationForSmoothingEntryToExitCMPSwitch());
      icpSingleSupportTrajectoryGenerator.setMinimumTimeToSpendOnExitCMP(icpPlannerParameters.getMinTimeToSpendOnExitCMPInSingleSupport());

      doubleSupportInitialTransferDuration.set(icpPlannerParameters.getDoubleSupportInitialTransferDuration());
      numberFootstepsToConsider.set(icpPlannerParameters.getNumberOfFootstepsToConsider());
      doubleSupportSplitFraction.set(icpPlannerParameters.getDoubleSupportSplitFraction());
      exitCMPDurationInPercentOfStepTime.set(icpPlannerParameters.getTimeSpentOnExitCMPInPercentOfStepTime());
      useTwoConstantCMPsPerSupport.set(icpPlannerParameters.useTwoCMPsPerSupport());

      velocityDecayDurationWhenDone.set(icpPlannerParameters.getVelocityDecayDurationWhenDone());
      velocityReductionFactor.set(Double.NaN);

      // Initialize omega0 to NaN to force the user to explicitly set it.
      omega0.set(Double.NaN);

      minSingleSupportDuration.set(0.3); // TODO Need to be extracted

      referenceCMPsCalculator = new ReferenceCentroidalMomentumPivotLocationsCalculator(namePrefix, bipedSupportPolygons, contactableFeet,
            numberFootstepsToConsider.getIntegerValue(), registry);
      referenceCMPsCalculator.initializeParameters(icpPlannerParameters);

      midFeetZUpFrame = bipedSupportPolygons.getMidFeetZUpFrame();
      soleZUpFrames = bipedSupportPolygons.getSoleZUpFrames();

      ReferenceFrame[] framesToRegister = new ReferenceFrame[] {worldFrame, midFeetZUpFrame, soleZUpFrames.get(RobotSide.LEFT), soleZUpFrames.get(RobotSide.RIGHT)};
      singleSupportInitialICP = new YoFramePointInMultipleFrames(namePrefix + "SingleSupportInitialICP", registry, framesToRegister);
      singleSupportFinalICP = new YoFramePointInMultipleFrames(namePrefix + "SingleSupportFinalICP", registry, framesToRegister);

      for (int i = 0; i < numberFootstepsToConsider.getIntegerValue() - 1; i++)
      {
         YoFramePointInMultipleFrames earlyCornerPoint = new YoFramePointInMultipleFrames(namePrefix + "EntryCornerPoints" + i, registry, framesToRegister);
         entryCornerPoints.add(earlyCornerPoint);

         YoFramePointInMultipleFrames lateCornerPoint = new YoFramePointInMultipleFrames(namePrefix + "ExitCornerPoints" + i, registry, framesToRegister);
         exitCornerPoints.add(lateCornerPoint);
      }

      parentRegistry.addChild(registry);

      if (yoGraphicsListRegistry != null)
      {
         setupVisualizers(yoGraphicsListRegistry);
      }
   }

   private void setupVisualizers(YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      YoGraphicsList yoGraphicsList = new YoGraphicsList(getClass().getSimpleName());
      ArtifactList artifactList = new ArtifactList(getClass().getSimpleName());

      referenceCMPsCalculator.createVisualizerForConstantCMPs(yoGraphicsList, artifactList);

      for (int i = 0; i < numberFootstepsToConsider.getIntegerValue() - 1; i++)
      {
         YoGraphicPosition icpEarlyCornerPointsViz = new YoGraphicPosition("EntryCornerPoints" + i,
               entryCornerPoints.get(i).buildUpdatedYoFramePointForVisualizationOnly(), ICP_CORNER_POINT_SIZE, YoAppearance.Blue(), GraphicType.SOLID_BALL);

         yoGraphicsList.add(icpEarlyCornerPointsViz);
         artifactList.add(icpEarlyCornerPointsViz.createArtifact());

         YoGraphicPosition lateCornerPointsViz = new YoGraphicPosition("ExitCornerPoints" + i,
               exitCornerPoints.get(i).buildUpdatedYoFramePointForVisualizationOnly(), ICP_CORNER_POINT_SIZE, YoAppearance.Blue(), GraphicType.BALL);

         yoGraphicsList.add(lateCornerPointsViz);
         artifactList.add(lateCornerPointsViz.createArtifact());
      }

      YoGraphicPosition singleSupportInitialICPViz = new YoGraphicPosition("singleSupportInitialICP",
            singleSupportInitialICP.buildUpdatedYoFramePointForVisualizationOnly(), 0.004, YoAppearance.Chocolate(), GraphicType.SOLID_BALL);
      yoGraphicsList.add(singleSupportInitialICPViz);
      artifactList.add(singleSupportInitialICPViz.createArtifact());

      YoGraphicPosition singleSupportFinalICPViz = new YoGraphicPosition("singleSupportFinalICP",
            singleSupportFinalICP.buildUpdatedYoFramePointForVisualizationOnly(), 0.004, YoAppearance.Chocolate(), GraphicType.BALL);
      yoGraphicsList.add(singleSupportFinalICPViz);
      artifactList.add(singleSupportFinalICPViz.createArtifact());

      icpSingleSupportTrajectoryGenerator.createVisualizers(yoGraphicsList, artifactList);

      artifactList.setVisible(VISUALIZE);
      yoGraphicsList.setVisible(VISUALIZE);

      yoGraphicsListRegistry.registerYoGraphicsList(yoGraphicsList);
      yoGraphicsListRegistry.registerArtifactList(artifactList);
   }

   public void clearPlan()
   {
      referenceCMPsCalculator.clear();
   }

   public void setSupportLeg(RobotSide robotSide)
   {
      supportSide.set(robotSide);
   }

   public void setTransferToSide(RobotSide robotSide)
   {
      transferToSide.set(robotSide);
   }

   public void setTransferFromSide(RobotSide robotSide)
   {
      if (robotSide != null)
         transferToSide.set(robotSide.getOppositeSide());
   }

   public void addFootstepToPlan(Footstep footstep)
   {
      referenceCMPsCalculator.addUpcomingFootstep(footstep);
   }

   public void setDesiredCapturePointState(FramePoint currentDesiredCapturePointPosition, FrameVector currentDesiredCapturePointVelocity)
   {
      desiredCapturePointPosition.set(currentDesiredCapturePointPosition);
      desiredCapturePointVelocity.set(currentDesiredCapturePointVelocity);
   }

   public void setDesiredCapturePointState(YoFramePoint currentDesiredCapturePointPosition, YoFrameVector currentDesiredCapturePointVelocity)
   {
      desiredCapturePointPosition.set(currentDesiredCapturePointPosition);
      desiredCapturePointVelocity.set(currentDesiredCapturePointVelocity);
   }

   public void setDesiredCapturePointState(FramePoint2d currentDesiredCapturePointPosition, FrameVector2d currentDesiredCapturePointVelocity)
   {
      // Do not set the Z to zero!
      desiredCapturePointPosition.checkReferenceFrameMatch(currentDesiredCapturePointPosition);
      desiredCapturePointPosition.setX(currentDesiredCapturePointPosition.getX());
      desiredCapturePointPosition.setY(currentDesiredCapturePointPosition.getY());

      desiredCapturePointVelocity.checkReferenceFrameMatch(currentDesiredCapturePointVelocity);
      desiredCapturePointVelocity.setX(currentDesiredCapturePointVelocity.getX());
      desiredCapturePointVelocity.setY(currentDesiredCapturePointVelocity.getY());
   }

   public void setDesiredCapturePointState(YoFramePoint2d currentDesiredCapturePointPosition, YoFrameVector2d currentDesiredCapturePointVelocity)
   {
      // Do not set the Z to zero!
      desiredCapturePointPosition.checkReferenceFrameMatch(currentDesiredCapturePointPosition);
      desiredCapturePointPosition.setX(currentDesiredCapturePointPosition.getX());
      desiredCapturePointPosition.setY(currentDesiredCapturePointPosition.getY());

      desiredCapturePointVelocity.checkReferenceFrameMatch(currentDesiredCapturePointVelocity);
      desiredCapturePointVelocity.setX(currentDesiredCapturePointVelocity.getX());
      desiredCapturePointVelocity.setY(currentDesiredCapturePointVelocity.getY());
   }

   public void holdCurrentICP(double initialTime, FramePoint actualICPToHold)
   {
      this.actualICPToHold.set(actualICPToHold);
      requestedHoldPosition.set(true);
   }

   public void initializeForStanding(double initialTime)
   {
      clearPlan();
      isStanding.set(true);
      isDoubleSupport.set(true);
      this.initialTime.set(initialTime);
      updateTransferPlan();
   }

   public void initializeForTransfer(double initialTime)
   {
      isDoubleSupport.set(true);
      this.initialTime.set(initialTime);
      updateTransferPlan();
   }

   private void updateTransferPlan()
   {
      RobotSide transferToSide = this.transferToSide.getEnumValue();
      if (transferToSide == null)
         transferToSide = RobotSide.LEFT;
      RobotSide transferFromSide = transferToSide.getOppositeSide();
      ReferenceFrame transferFromSoleFrame = soleZUpFrames.get(transferFromSide);
      ReferenceFrame transferToSoleFrame = soleZUpFrames.get(transferToSide);

      icpSingleSupportTrajectoryGenerator.hideVisualization();

      referenceCMPsCalculator.setUseTwoCMPsPerSupport(useTwoConstantCMPsPerSupport.getBooleanValue());
      referenceCMPsCalculator.computeReferenceCMPsStartingFromDoubleSupport(isStanding.getBooleanValue(), transferToSide);
      referenceCMPsCalculator.update();
      ArrayList<YoFramePoint> entryCMPs = referenceCMPsCalculator.getEntryCMPs();
      ArrayList<YoFramePoint> exitCMPs = referenceCMPsCalculator.getExitCMPs();
      double steppingDuration = doubleSupportDuration.getDoubleValue() + singleSupportDuration.getDoubleValue();
      double doubleSupportTimeSpentBeforeEntryCornerPoint = doubleSupportDuration.getDoubleValue() * doubleSupportSplitFraction.getDoubleValue();
      double doubleSupportTimeSpentAfterEntryCornerPoint = doubleSupportDuration.getDoubleValue() * (1.0 - doubleSupportSplitFraction.getDoubleValue());
      switchCornerPointsToWorldFrame();
      singleSupportInitialICP.switchCurrentReferenceFrame(worldFrame);
      singleSupportFinalICP.switchCurrentReferenceFrame(worldFrame);

      boolean isDoneWalking = referenceCMPsCalculator.isDoneWalking();

      ReferenceFrame initialFrame;

      if (isStanding.getBooleanValue())
      {
         initialFrame = midFeetZUpFrame;
      }
      else
      {
         tempICP.setToZero(midFeetZUpFrame);
         tempICP.changeFrame(worldFrame);
         double distanceFromDesiredICPToMidfeetZUpFrame = desiredCapturePointPosition.getXYPlaneDistance(tempICP);
         tempICP.setToZero(transferFromSoleFrame);
         tempICP.changeFrame(worldFrame);
         double distanceFromDesiredICPToTransferFromSoleFrame = desiredCapturePointPosition.getXYPlaneDistance(tempICP);

         if (distanceFromDesiredICPToMidfeetZUpFrame < distanceFromDesiredICPToTransferFromSoleFrame)
            initialFrame = midFeetZUpFrame;
         else
            initialFrame = transferFromSoleFrame;
      }
      ReferenceFrame finalFrame = isDoneWalking ? midFeetZUpFrame : transferToSoleFrame;

      if (requestedHoldPosition.getBooleanValue())
      {
         desiredCapturePointPosition.set(actualICPToHold);
         desiredCapturePointVelocity.setToZero();
         singleSupportInitialICP.setIncludingFrame(actualICPToHold);
         singleSupportFinalICP.setIncludingFrame(actualICPToHold);
         singleSupportInitialICPVelocity.set(0.0, 0.0, 0.0);
         setCornerPointsToNaN();
         actualICPToHold.setToNaN();
         isDoneWalking = true;
         requestedHoldPosition.set(false);
         isHoldingPosition.set(true);
      }
      else if (isDoneWalking)
      {
         singleSupportInitialICP.setIncludingFrame(entryCMPs.get(0));
         singleSupportFinalICP.setIncludingFrame(singleSupportInitialICP);
         singleSupportInitialICPVelocity.set(0.0, 0.0, 0.0);
         setCornerPointsToNaN();
         isHoldingPosition.set(false);
      }
      else
      {
         double omega0 = this.omega0.getDoubleValue();

         if (useTwoConstantCMPsPerSupport.getBooleanValue())
         {
            double exitPointToDoubleSupportDuration = steppingDuration * exitCMPDurationInPercentOfStepTime.getDoubleValue() - doubleSupportTimeSpentAfterEntryCornerPoint;

            computeDesiredCornerPoints(entryCornerPoints, exitCornerPoints, entryCMPs, exitCMPs, steppingDuration, exitCMPDurationInPercentOfStepTime.getDoubleValue(), omega0);
            computeDesiredCapturePointPosition(omega0, exitPointToDoubleSupportDuration, exitCornerPoints.get(1), exitCMPs.get(1), singleSupportFinalICP);
            exitCornerPoints.get(0).changeFrame(initialFrame);
            exitCornerPoints.get(1).changeFrame(finalFrame);
         }
         else
         {
            computeDesiredCornerPoints(entryCornerPoints, entryCMPs, false, steppingDuration, omega0);
            double timeToNextCornerPoint = doubleSupportTimeSpentBeforeEntryCornerPoint + singleSupportDuration.getDoubleValue();
            computeDesiredCapturePointPosition(omega0, timeToNextCornerPoint, entryCornerPoints.get(1), entryCMPs.get(1), singleSupportFinalICP);
         }

         computeDesiredCapturePointPosition(omega0, doubleSupportTimeSpentBeforeEntryCornerPoint, entryCornerPoints.get(1), entryCMPs.get(1), singleSupportInitialICP);
         computeDesiredCapturePointVelocity(omega0, 0.0, singleSupportInitialICP, entryCMPs.get(1), singleSupportInitialICPVelocity);

         entryCornerPoints.get(0).changeFrame(initialFrame);
         entryCornerPoints.get(1).changeFrame(finalFrame);
         changeFrameOfRemainingCornerPoints(2, worldFrame);
         isHoldingPosition.set(false);
      }

      singleSupportInitialICP.changeFrame(finalFrame);
      singleSupportFinalICP.changeFrame(worldFrame);

      if (isStanding.getBooleanValue() && !isDoneWalking)
      {
         isInitialTransfer.set(true);
         isStanding.set(false);
      }

      DoubleYoVariable doubleSupportTimeToUse = isInitialTransfer.getBooleanValue() ? doubleSupportInitialTransferDuration : doubleSupportDuration;

      icpDoubleSupportTrajectoryGenerator.setTrajectoryTime(doubleSupportTimeToUse.getDoubleValue());
      icpDoubleSupportTrajectoryGenerator.setInitialConditions(desiredCapturePointPosition, desiredCapturePointVelocity, initialFrame);
      icpDoubleSupportTrajectoryGenerator.setFinalConditions(singleSupportInitialICP, singleSupportInitialICPVelocity, finalFrame);
      icpDoubleSupportTrajectoryGenerator.initialize();
   }

   public void initializeForSingleSupport(double initialTime)
   {
      isHoldingPosition.set(false);

      isStanding.set(false);
      isInitialTransfer.set(false);
      isDoubleSupport.set(false);
      this.initialTime.set(initialTime);

      updateSingleSupportPlan();
   }

   private void updateSingleSupportPlan()
   {
      RobotSide supportSide = this.supportSide.getEnumValue();

      referenceCMPsCalculator.setUseTwoCMPsPerSupport(useTwoConstantCMPsPerSupport.getBooleanValue());
      referenceCMPsCalculator.computeReferenceCMPsStartingFromSingleSupport(supportSide);
      referenceCMPsCalculator.update();
      ArrayList<YoFramePoint> entryCMPs = referenceCMPsCalculator.getEntryCMPs();
      ArrayList<YoFramePoint> exitCMPs = referenceCMPsCalculator.getExitCMPs();
      double steppingDuration = doubleSupportDuration.getDoubleValue() + singleSupportDuration.getDoubleValue();
      double doubleSupportTimeSpentBeforeEntryCornerPoint = doubleSupportDuration.getDoubleValue() * doubleSupportSplitFraction.getDoubleValue();
      double doubleSupportTimeSpentAfterEntryCornerPoint = doubleSupportDuration.getDoubleValue() * (1.0 - doubleSupportSplitFraction.getDoubleValue());
      double totalTimeSpentOnExitCMP = steppingDuration * exitCMPDurationInPercentOfStepTime.getDoubleValue();
      double totalTimeSpentOnEntryCMP = steppingDuration * (1.0 - exitCMPDurationInPercentOfStepTime.getDoubleValue());

      switchCornerPointsToWorldFrame();
      singleSupportInitialICP.switchCurrentReferenceFrame(worldFrame);
      singleSupportFinalICP.switchCurrentReferenceFrame(worldFrame);

      ReferenceFrame supportSoleFrame = soleZUpFrames.get(supportSide);
      double omega0 = this.omega0.getDoubleValue();
      if (useTwoConstantCMPsPerSupport.getBooleanValue())
      {
         double timeRemainingOnEntryCMP = totalTimeSpentOnEntryCMP - doubleSupportTimeSpentBeforeEntryCornerPoint;
         double timeToSpendOnFinalCMPBeforeDoubleSupport = totalTimeSpentOnExitCMP - doubleSupportTimeSpentAfterEntryCornerPoint;

         computeDesiredCornerPoints(entryCornerPoints, exitCornerPoints, entryCMPs, exitCMPs, steppingDuration, exitCMPDurationInPercentOfStepTime.getDoubleValue(), omega0);
         computeDesiredCapturePointPosition(omega0, doubleSupportTimeSpentBeforeEntryCornerPoint, entryCornerPoints.get(0), entryCMPs.get(0), singleSupportInitialICP);
         computeDesiredCapturePointVelocity(omega0, doubleSupportTimeSpentBeforeEntryCornerPoint, entryCornerPoints.get(0), entryCMPs.get(0), singleSupportInitialICPVelocity);

         computeDesiredCapturePointPosition(omega0, timeToSpendOnFinalCMPBeforeDoubleSupport, exitCornerPoints.get(0), exitCMPs.get(0), singleSupportFinalICP);
         computeDesiredCapturePointVelocity(omega0, timeToSpendOnFinalCMPBeforeDoubleSupport, exitCornerPoints.get(0), exitCMPs.get(0), singleSupportFinalICPVelocity);

         icpSingleSupportTrajectoryGenerator.setBoundaryICP(singleSupportInitialICP, singleSupportFinalICP);
         icpSingleSupportTrajectoryGenerator.setCornerPoints(entryCornerPoints.get(0), exitCornerPoints.get(0));
         icpSingleSupportTrajectoryGenerator.setReferenceCMPs(entryCMPs.get(0), exitCMPs.get(0));
         icpSingleSupportTrajectoryGenerator.setReferenceFrames(supportSoleFrame, worldFrame);
         icpSingleSupportTrajectoryGenerator.setTrajectoryTime(timeRemainingOnEntryCMP, timeToSpendOnFinalCMPBeforeDoubleSupport);
         icpSingleSupportTrajectoryGenerator.initialize();

         exitCornerPoints.get(0).changeFrame(supportSoleFrame);
      }
      else
      {
         computeDesiredCornerPoints(entryCornerPoints, entryCMPs, false, steppingDuration, omega0);
         computeDesiredCapturePointPosition(omega0, doubleSupportTimeSpentBeforeEntryCornerPoint, entryCornerPoints.get(0), entryCMPs.get(0), singleSupportInitialICP);
         computeDesiredCapturePointPosition(omega0, doubleSupportTimeSpentBeforeEntryCornerPoint + singleSupportDuration.getDoubleValue(), entryCornerPoints.get(0), entryCMPs.get(0), singleSupportFinalICP);
      }

      singleSupportInitialICP.changeFrame(supportSoleFrame);
      entryCornerPoints.get(0).changeFrame(supportSoleFrame);
      singleSupportFinalICP.changeFrame(worldFrame);
      changeFrameOfRemainingCornerPoints(1, worldFrame);
   }

   private void setCornerPointsToNaN()
   {
      for (int i = 0; i < entryCornerPoints.size(); i++)
         entryCornerPoints.get(i).setToNaN();
      for (int i = 0; i < exitCornerPoints.size(); i++)
         exitCornerPoints.get(i).setToNaN();
   }

   private void switchCornerPointsToWorldFrame()
   {
      for (int i = 0; i < entryCornerPoints.size(); i++)
         entryCornerPoints.get(i).switchCurrentReferenceFrame(worldFrame);
      for (int i = 0; i < exitCornerPoints.size(); i++)
         exitCornerPoints.get(i).switchCurrentReferenceFrame(worldFrame);
   }

   private void changeFrameOfRemainingCornerPoints(int fromIndex, ReferenceFrame desiredFrame)
   {
      for (int i = fromIndex; i < entryCornerPoints.size(); i++)
         entryCornerPoints.get(i).changeFrame(desiredFrame);
      for (int i = fromIndex; i < exitCornerPoints.size(); i++)
         exitCornerPoints.get(i).changeFrame(desiredFrame);
   }

   public void updateCurrentPlan()
   {
      if (isDoubleSupport.getBooleanValue())
      {
         if (isHoldingPosition.getBooleanValue())
            requestedHoldPosition.set(true);
         updateTransferPlan();
      }
      else
      {
         updateSingleSupportPlan();
      }
   }

   public void updatePlanForSingleSupportDisturbances(double time, FramePoint2d actualCapturePointPosition)
   {
      initializeForSingleSupport(initialTime.getDoubleValue());

      if (isDone(time))
         return;

      double deltaTimeToBeAccounted = estimateDeltaTimeBetweenDesiredICPAndActualICP(time, actualCapturePointPosition);

      if (Double.isNaN(deltaTimeToBeAccounted))
         return;

      // Ensure that we don't shift the time by more than what's remaining
      deltaTimeToBeAccounted = Math.min(deltaTimeToBeAccounted, computeAndReturnTimeRemaining(time));
      // Ensure the time shift won't imply a single support that's crazy short
      deltaTimeToBeAccounted = Math.min(deltaTimeToBeAccounted, singleSupportDuration.getDoubleValue() - minSingleSupportDuration.getDoubleValue());

      initialTime.sub(deltaTimeToBeAccounted);
   }

   public double estimateTimeRemainingForStateUnderDisturbance(double time, FramePoint2d actualCapturePointPosition)
   {
      if (isDone(time))
         return 0.0;

      double deltaTimeToBeAccounted = estimateDeltaTimeBetweenDesiredICPAndActualICP(time, actualCapturePointPosition);

      if (Double.isNaN(deltaTimeToBeAccounted))
         return 0.0;

      double estimatedTimeRemaining = computeAndReturnTimeRemaining(time) - deltaTimeToBeAccounted;
      estimatedTimeRemaining = MathTools.clipToMinMax(estimatedTimeRemaining, 0.0, Double.POSITIVE_INFINITY);

      return estimatedTimeRemaining;
   }

   private final FramePoint2d desiredICP2d = new FramePoint2d();
   private final FramePoint2d finalICP2d = new FramePoint2d();
   private final FrameLine2d desiredICPToFinalICPLine = new FrameLine2d();
   private final FrameLineSegment2d desiredICPToFinalICPLineSegment = new FrameLineSegment2d();
   private final FramePoint2d actualICP2d = new FramePoint2d();

   private double estimateDeltaTimeBetweenDesiredICPAndActualICP(double time, FramePoint2d actualCapturePointPosition)
   {
      computeDesiredCapturePoint(computeAndReturnTimeInCurrentState(time));
      computeDesiredCentroidalMomentumPivot();
      desiredCapturePointPosition.getFrameTuple2dIncludingFrame(desiredICP2d);
      singleSupportFinalICP.getFrameTuple2dIncludingFrame(finalICP2d);

      if (desiredICP2d.distance(finalICP2d) < 1.0e-10)
         return Double.NaN;

      desiredICPToFinalICPLineSegment.set(desiredICP2d, finalICP2d);
      actualICP2d.setIncludingFrame(actualCapturePointPosition);
      double percentAlongLineSegmentICP = desiredICPToFinalICPLineSegment.percentageAlongLineSegment(actualICP2d);
      if (percentAlongLineSegmentICP < 0.0)
      {
         desiredICPToFinalICPLine.set(desiredICP2d, finalICP2d);
         desiredICPToFinalICPLine.orthogonalProjection(actualICP2d);
      }
      else
      {
         desiredICPToFinalICPLineSegment.orthogonalProjection(actualICP2d);
      }

      double actualDistanceDueToDisturbance = desiredCentroidalMomentumPivotPosition.getXYPlaneDistance(actualICP2d);
      double expectedDistanceAccordingToPlan = desiredCentroidalMomentumPivotPosition.getXYPlaneDistance(desiredCapturePointPosition);

      computeTimeInCurrentState(time);
      double distanceRatio = actualDistanceDueToDisturbance / expectedDistanceAccordingToPlan;

      if (distanceRatio < 1.0e-3)
         return 0.0;
      else
         return Math.log(distanceRatio) / omega0.getDoubleValue();
   }

   private void computeDesiredCapturePoint(double timeInCurrentState)
   {
      update();
      referenceCMPsCalculator.update();

      if (isDoubleSupport.getBooleanValue())
      {
         icpDoubleSupportTrajectoryGenerator.compute(timeInCurrentState);
         icpDoubleSupportTrajectoryGenerator.get(desiredCapturePointPosition);
         icpDoubleSupportTrajectoryGenerator.getVelocity(desiredCapturePointVelocity);
         icpDoubleSupportTrajectoryGenerator.getAcceleration(desiredCapturePointAcceleration);
      }
      else if (useTwoConstantCMPsPerSupport.getBooleanValue())
      {
         icpSingleSupportTrajectoryGenerator.compute(timeInCurrentState);
         icpSingleSupportTrajectoryGenerator.get(desiredCapturePointPosition);
         icpSingleSupportTrajectoryGenerator.getVelocity(desiredCapturePointVelocity);
         icpSingleSupportTrajectoryGenerator.getAcceleration(desiredCapturePointAcceleration);
      }
      else
      {
         referenceCMPsCalculator.getNextEntryCMP(tempConstantCMP);
         singleSupportInitialICP.getFrameTupleIncludingFrame(tempICP);
         tempICP.changeFrame(worldFrame);
         timeInCurrentState = MathTools.clipToMinMax(timeInCurrentState, 0.0, singleSupportDuration.getDoubleValue());
         CapturePointTools.computeDesiredCapturePointPosition(omega0.getDoubleValue(), timeInCurrentState, tempICP, tempConstantCMP, desiredCapturePointPosition);
         CapturePointTools.computeDesiredCapturePointVelocity(omega0.getDoubleValue(), timeInCurrentState, tempICP, tempConstantCMP, desiredCapturePointVelocity);
         CapturePointTools.computeDesiredCapturePointAcceleration(omega0.getDoubleValue(), timeInCurrentState, tempICP, tempConstantCMP, desiredCapturePointAcceleration);
      }

      decayDesiredVelocityIfNeeded(timeInCurrentState);
   }

   private void decayDesiredVelocityIfNeeded(double timeInCurrentState)
   {
      if (velocityDecayDurationWhenDone.isNaN() || isStanding.getBooleanValue())
      {
         velocityReductionFactor.set(Double.NaN);
         return;
      }

      double hasBeenDoneForDuration = timeInCurrentState;
      if (isInitialTransfer.getBooleanValue())
         hasBeenDoneForDuration -= doubleSupportInitialTransferDuration.getDoubleValue();
      else if (isDoubleSupport.getBooleanValue())
         hasBeenDoneForDuration -= doubleSupportDuration.getDoubleValue();
      else
         hasBeenDoneForDuration -= singleSupportDuration.getDoubleValue();

      if (hasBeenDoneForDuration <= 0.0)
      {
         velocityReductionFactor.set(Double.NaN);
      }
      else
      {
         velocityReductionFactor.set(MathTools.clipToMinMax(1.0 - hasBeenDoneForDuration / velocityDecayDurationWhenDone.getDoubleValue(), 0.0, 1.0));
         desiredCapturePointVelocity.scale(velocityReductionFactor.getDoubleValue());
      }
   }

   private void update()
   {
      singleSupportInitialICP.notifyVariableChangedListeners();
      singleSupportFinalICP.notifyVariableChangedListeners();

      for (int i = 0; i < entryCornerPoints.size(); i++)
         entryCornerPoints.get(i).notifyVariableChangedListeners();
      for (int i = 0; i < exitCornerPoints.size(); i++)
         exitCornerPoints.get(i).notifyVariableChangedListeners();
   }

   private void computeDesiredCentroidalMomentumPivot()
   {
      CapturePointTools.computeDesiredCentroidalMomentumPivot(desiredCapturePointPosition, desiredCapturePointVelocity, omega0.getDoubleValue(),
            desiredCentroidalMomentumPivotPosition);
   }

   public void getDesiredCapturePointPositionAndVelocity(FramePoint2d desiredCapturePointPositionToPack, FrameVector2d desiredCapturePointVelocityToPack,
         double time)
   {
      computeTimeInCurrentState(time);
      computeDesiredCapturePoint(timeInCurrentState.getDoubleValue());

      desiredCapturePointPosition.getFrameTuple2dIncludingFrame(desiredCapturePointPositionToPack);
      desiredCapturePointVelocity.getFrameTuple2dIncludingFrame(desiredCapturePointVelocityToPack);
   }

   public void getDesiredCapturePointPositionAndVelocity(FramePoint desiredCapturePointPositionToPack, FrameVector desiredCapturePointVelocityToPack,
         double time)
   {
      computeTimeInCurrentState(time);
      computeDesiredCapturePoint(timeInCurrentState.getDoubleValue());

      desiredCapturePointPosition.getFrameTupleIncludingFrame(desiredCapturePointPositionToPack);
      desiredCapturePointVelocity.getFrameTupleIncludingFrame(desiredCapturePointVelocityToPack);
   }

   public void getDesiredCapturePointPositionAndVelocity(YoFramePoint desiredCapturePointPositionToPack, YoFrameVector desiredCapturePointVelocityToPack,
         double time)
   {
      computeTimeInCurrentState(time);
      computeDesiredCapturePoint(timeInCurrentState.getDoubleValue());

      desiredCapturePointPositionToPack.set(desiredCapturePointPosition);
      desiredCapturePointVelocityToPack.set(desiredCapturePointVelocity);
   }

   public void getDesiredCentroidalMomentumPivotPosition(FramePoint desiredCentroidalMomentumPivotPositionToPack)
   {
      computeDesiredCentroidalMomentumPivot();
      desiredCentroidalMomentumPivotPosition.getFrameTupleIncludingFrame(desiredCentroidalMomentumPivotPositionToPack);
   }

   public void getDesiredCentroidalMomentumPivotPosition(FramePoint2d desiredCentroidalMomentumPivotPositionToPack)
   {
      computeDesiredCentroidalMomentumPivot();
      desiredCentroidalMomentumPivotPosition.getFrameTuple2dIncludingFrame(desiredCentroidalMomentumPivotPositionToPack);
   }

   public void getDesiredCentroidalMomentumPivotVelocity(FrameVector desiredCentroidalMomentumPivotVelocityToPack)
   {
      CapturePointTools.computeDesiredCentroidalMomentumPivotVelocity(desiredCapturePointVelocity, desiredCapturePointAcceleration, omega0.getDoubleValue(), desiredCentroidalMomentumPivotVelocity);
      desiredCentroidalMomentumPivotVelocity.getFrameTupleIncludingFrame(desiredCentroidalMomentumPivotVelocityToPack);
   }

   public void getDesiredCentroidalMomentumPivotVelocity(FrameVector2d desiredCentroidalMomentumPivotVelocityToPack)
   {
      CapturePointTools.computeDesiredCentroidalMomentumPivotVelocity(desiredCapturePointVelocity, desiredCapturePointAcceleration, omega0.getDoubleValue(), desiredCentroidalMomentumPivotVelocity);
      desiredCentroidalMomentumPivotVelocity.getFrameTuple2dIncludingFrame(desiredCentroidalMomentumPivotVelocityToPack);
   }

   protected void computeTimeInCurrentState(double time)
   {
      timeInCurrentState.set(time - initialTime.getDoubleValue());
   }

   public double computeAndReturnTimeRemaining(double time)
   {
      computeTimeInCurrentState(time);

      DoubleYoVariable stateDuration;

      if (isDoubleSupport.getBooleanValue())
         stateDuration = isInitialTransfer.getBooleanValue() ? doubleSupportInitialTransferDuration : doubleSupportDuration;
      else
         stateDuration = singleSupportDuration;

      remainingTime.set(stateDuration.getDoubleValue() - timeInCurrentState.getDoubleValue());
      return remainingTime.getDoubleValue();
   }

   public void setInitialDoubleSupportTime(double time)
   {
      doubleSupportInitialTransferDuration.set(time);
   }

   public void setDoubleSupportTime(double time)
   {
      doubleSupportDuration.set(time);
   }

   public void setSingleSupportTime(double time)
   {
      singleSupportDuration.set(time);
   }

   public void setMinimumSingleSupportTimeForDisturbanceRecovery(double minTime)
   {
      minSingleSupportDuration.set(minTime);
   }

   public double getInitialTransferDuration()
   {
      return doubleSupportInitialTransferDuration.getDoubleValue();
   }

   public void setDoubleSupportSplitFraction(double doubleSupportSplitFraction)
   {
      this.doubleSupportSplitFraction.set(doubleSupportSplitFraction);
   }

   public void setOmega0(double omega0)
   {
      this.omega0.set(omega0);
   }

   public boolean isInDoubleSupport()
   {
      return isDoubleSupport.getBooleanValue();
   }

   public boolean isInStanding()
   {
      return isStanding.getBooleanValue();
   }

   public boolean isInInitialTranfer()
   {
      return isInitialTransfer.getBooleanValue();
   }

   public void getSingleSupportInitialCapturePointPosition(FramePoint capturePointPositionToPack)
   {
      singleSupportInitialICP.getFrameTupleIncludingFrame(capturePointPositionToPack);
   }

   public double computeAndReturnTimeInCurrentState(double time)
   {
      computeTimeInCurrentState(time);
      return timeInCurrentState.getDoubleValue();
   }

   public void getFinalDesiredCapturePointPosition(FramePoint finalDesiredCapturePointPositionToPack)
   {
      if (isStanding.getBooleanValue())
         referenceCMPsCalculator.getNextEntryCMP(tempFinalICP);
      else
         entryCornerPoints.get(1).getFrameTupleIncludingFrame(tempFinalICP);
      tempFinalICP.changeFrame(worldFrame);
      finalDesiredCapturePointPositionToPack.setIncludingFrame(tempFinalICP);
   }

   private final FramePoint tempFinalICP = new FramePoint();

   public void getFinalDesiredCapturePointPosition(YoFramePoint2d finalDesiredCapturePointPositionToPack)
   {
      if (isStanding.getBooleanValue())
         referenceCMPsCalculator.getNextEntryCMP(tempFinalICP);
      else
         entryCornerPoints.get(1).getFrameTupleIncludingFrame(tempFinalICP);
      tempFinalICP.changeFrame(finalDesiredCapturePointPositionToPack.getReferenceFrame());
      finalDesiredCapturePointPositionToPack.setByProjectionOntoXYPlane(tempFinalICP);
   }

   public void getNextExitCMP(FramePoint entryCMPToPack)
   {
      referenceCMPsCalculator.getNextExitCMP(entryCMPToPack);
   }

   public boolean isDone(double time)
   {
      return computeAndReturnTimeRemaining(time) <= 0.0;
   }

   public boolean isOnExitCMP()
   {
      if (isDoubleSupport.getBooleanValue())
         return false;
      else
         return icpSingleSupportTrajectoryGenerator.isOnExitCMP();
   }
}
