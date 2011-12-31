/*
 * $RCSfile$
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 *
 * $Revision$
 * $Date$
 * $State$
 */

package com.sun.j3d.utils.pickfast.behaviors;

import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.PickInfo;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.pickfast.PickTool;

/**
 * A mouse behavior that allows user to pick and rotate scene graph objects.
 * Common usage:
 * <p>
 * 1. Create your scene graph.
 * <p>
 * 2. Create this behavior with root and canvas.
 * <p>
 * <blockquote><pre>
 *	PickRotateBehavior behavior = new PickRotateBehavior(canvas, root, bounds);
 *      root.addChild(behavior);
 * </pre></blockquote>
 * <p>
 * The above behavior will monitor for any picking events on
 * the scene graph (below root node) and handle mouse rotates on pick hits.
 * Note the root node can also be a subgraph node of the scene graph (rather
 * than the topmost).
 */

public class PickRotateBehavior extends PickMouseBehavior implements MouseBehaviorCallback {
    MouseRotate rotate;
    private PickingCallback callback=null;
    private TransformGroup currentTG;

    /**
     * Creates a pick/rotate behavior that waits for user mouse events for
     * the scene graph. This method has its pickMode set to BOUNDS picking.
     * @param root   Root of your scene graph.
     * @param canvas Java 3D drawing canvas.
     * @param bounds Bounds of your scene.
     **/

    public PickRotateBehavior(BranchGroup root, Canvas3D canvas, Bounds bounds){
	super(canvas, root, bounds);
	rotate = new MouseRotate(MouseRotate.MANUAL_WAKEUP);
	rotate.setTransformGroup(currGrp);
	currGrp.addChild(rotate);
	rotate.setSchedulingBounds(bounds);
	this.setSchedulingBounds(bounds);
    }

    /**
     * Creates a pick/rotate behavior that waits for user mouse events for
     * the scene graph.
     * @param root   Root of your scene graph.
     * @param canvas Java 3D drawing canvas.
     * @param bounds Bounds of your scene.
     * @param pickMode specifys PickTool.PICK_BOUNDS or PickTool.PICK_GEOMETRY.
     * @see PickTool#setMode
     **/

    public PickRotateBehavior(BranchGroup root, Canvas3D canvas, Bounds bounds,
			      int pickMode){
	super(canvas, root, bounds);
	rotate = new MouseRotate(MouseRotate.MANUAL_WAKEUP);
	rotate.setTransformGroup(currGrp);
	currGrp.addChild(rotate);
	rotate.setSchedulingBounds(bounds);
	this.setSchedulingBounds(bounds);
	this.setMode(pickMode);
    }


    /**
     * Update the scene to manipulate any nodes. This is not meant to be
     * called by users. Behavior automatically calls this. You can call
     * this only if you know what you are doing.
     *
     * @param xpos Current mouse X pos.
     * @param ypos Current mouse Y pos.
     **/
    public void updateScene(int xpos, int ypos) {
	TransformGroup tg = null;

	if (!mevent.isMetaDown() && !mevent.isAltDown()){

	    // System.out.println("PickRotateBeh : using pickfast pkg : PickInfo ...");
	    pickCanvas.setFlags(PickInfo.NODE | PickInfo.SCENEGRAPHPATH);

	    pickCanvas.setShapeLocation(xpos, ypos);
	    PickInfo pickInfo = pickCanvas.pickClosest();
	    if(pickInfo != null) {
		// System.out.println("Intersected!");
		tg = (TransformGroup) pickCanvas.getNode(pickInfo, PickTool.TYPE_TRANSFORM_GROUP);
		if((tg != null) &&
		   (tg.getCapability(TransformGroup.ALLOW_TRANSFORM_READ)) &&
		   (tg.getCapability(TransformGroup.ALLOW_TRANSFORM_WRITE))) {
		    rotate.setTransformGroup(tg);
		    rotate.wakeup();
		    currentTG = tg;
		}
	    } else if (callback!=null)
		callback.transformChanged( PickingCallback.NO_PICK, null );
	}
    }

    /**
     * Callback method from MouseRotate
     * This is used when the Picking callback is enabled
     */
    public void transformChanged( int type, Transform3D transform ) {
	callback.transformChanged( PickingCallback.ROTATE, currentTG );
    }

    /**
     * Register the class @param callback to be called each
     * time the picked object moves
     */
    public void setupCallback( PickingCallback callback ) {
	this.callback = callback;
	if (callback==null)
	    rotate.setupCallback( null );
	else
	    rotate.setupCallback( this );
    }

}

