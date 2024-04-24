package gov.nih.nci.evs.restapi.bean;

import java.io.*;
import java.util.*;


public class ResolvedConceptReferenceList implements java.io.Serializable {

    private Boolean _incomplete = Boolean.valueOf("false");
    private java.util.List<ResolvedConceptReference> _resolvedConceptReferenceList;


    public ResolvedConceptReferenceList() {
        super();
        this._resolvedConceptReferenceList = new java.util.ArrayList<ResolvedConceptReference>();
    }


    public void addResolvedConceptReference(
            final ResolvedConceptReference vResolvedConceptReference)
    throws java.lang.IndexOutOfBoundsException {
        this._resolvedConceptReferenceList.add(vResolvedConceptReference);
    }

    public void addResolvedConceptReference(
            final int index,
            final ResolvedConceptReference vResolvedConceptReference)
    throws java.lang.IndexOutOfBoundsException {
        this._resolvedConceptReferenceList.add(index, vResolvedConceptReference);
    }

    public java.util.Enumeration<? extends ResolvedConceptReference> enumerateResolvedConceptReference(
    ) {
        return java.util.Collections.enumeration(this._resolvedConceptReferenceList);
    }

    public java.lang.Boolean getIncomplete(
    ) {
        return this._incomplete;
    }

    public ResolvedConceptReference getResolvedConceptReference(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._resolvedConceptReferenceList.size()) {
            throw new IndexOutOfBoundsException("getResolvedConceptReference: Index value '" + index + "' not in range [0.." + (this._resolvedConceptReferenceList.size() - 1) + "]");
        }
        return (ResolvedConceptReference) _resolvedConceptReferenceList.get(index);
    }

    public ResolvedConceptReference[] getResolvedConceptReference(
    ) {
        ResolvedConceptReference[] array = new ResolvedConceptReference[0];
        return (ResolvedConceptReference[]) this._resolvedConceptReferenceList.toArray(array);
    }

    public int getResolvedConceptReferenceCount(
    ) {
        return this._resolvedConceptReferenceList.size();
    }

    public java.lang.Boolean isIncomplete(
    ) {
        return this._incomplete;
    }

    public java.util.Iterator<? extends ResolvedConceptReference> iterateResolvedConceptReference(
    ) {
        return this._resolvedConceptReferenceList.iterator();
    }

    public void removeAllResolvedConceptReference(
    ) {
        this._resolvedConceptReferenceList.clear();
    }

    public boolean removeResolvedConceptReference(
            final ResolvedConceptReference vResolvedConceptReference) {
        boolean removed = _resolvedConceptReferenceList.remove(vResolvedConceptReference);
        return removed;
    }

    public ResolvedConceptReference removeResolvedConceptReferenceAt(
            final int index) {
        java.lang.Object obj = this._resolvedConceptReferenceList.remove(index);
        return (ResolvedConceptReference) obj;
    }

    public void setIncomplete(
            final java.lang.Boolean incomplete) {
        this._incomplete = incomplete;
    }

    public void setResolvedConceptReference(
            final int index,
            final ResolvedConceptReference vResolvedConceptReference)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._resolvedConceptReferenceList.size()) {
            throw new IndexOutOfBoundsException("setResolvedConceptReference: Index value '" + index + "' not in range [0.." + (this._resolvedConceptReferenceList.size() - 1) + "]");
        }
        this._resolvedConceptReferenceList.set(index, vResolvedConceptReference);
    }

    public void setResolvedConceptReference(
            final ResolvedConceptReference[] vResolvedConceptReferenceArray) {
        _resolvedConceptReferenceList.clear();

        for (int i = 0; i < vResolvedConceptReferenceArray.length; i++) {
                this._resolvedConceptReferenceList.add(vResolvedConceptReferenceArray[i]);
        }
    }
}
