/*
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
 */

package org.jogamp.java3d.utils.scenegraph.io.state.org.jogamp.java3d;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.jogamp.java3d.SceneGraphObject;
import org.jogamp.java3d.TransparencyAttributes;
import org.jogamp.java3d.TransparencyInterpolator;

import org.jogamp.java3d.utils.scenegraph.io.retained.Controller;
import org.jogamp.java3d.utils.scenegraph.io.retained.SymbolTableData;

public class TransparencyInterpolatorState extends InterpolatorState {

    private int target;

    public TransparencyInterpolatorState(SymbolTableData symbol,Controller control) {
        super( symbol, control );

        if (node!=null)
            target = control.getSymbolTable().addReference( ((TransparencyInterpolator)node).getTarget() );
    }

    @Override
    public void writeObject( DataOutput out ) throws IOException {
        super.writeObject( out );

        out.writeInt( target );
        out.writeFloat( ((TransparencyInterpolator)node).getMinimumTransparency() );
        out.writeFloat( ((TransparencyInterpolator)node).getMaximumTransparency() );
    }

    @Override
    public void readObject( DataInput in ) throws IOException {
        super.readObject( in );

        target = in.readInt();
        ((TransparencyInterpolator)node).setMinimumTransparency( in.readFloat() );
        ((TransparencyInterpolator)node).setMaximumTransparency( in.readFloat() );
    }

    /**
     * Called when this component reference count is incremented.
     * Allows this component to update the reference count of any components
     * that it references.
     */
    @Override
    public void addSubReference() {
        control.getSymbolTable().incNodeComponentRefCount( target );
    }

    @Override
    public void buildGraph() {
        ((TransparencyInterpolator)node).setTarget( (TransparencyAttributes)control.getSymbolTable().getJ3dNode( target ));
        super.buildGraph(); // Must be last call in method
    }

    @Override
    public SceneGraphObject createNode( Class j3dClass ) {
        return createNode( j3dClass, new Class[] { org.jogamp.java3d.Alpha.class,
                                                    org.jogamp.java3d.TransparencyAttributes.class },
                                      new Object[] { null,
                                                     null } );
    }

    @Override
    protected org.jogamp.java3d.SceneGraphObject createNode() {
        return new TransparencyInterpolator( null, null );
    }


}
