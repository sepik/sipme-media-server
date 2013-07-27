/*
 *  Copyright (c) 2011 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

/******************************************************************

 iLBC Speech Coder ANSI-C Source Code

 WebRtcIlbcfix_DecodeResidual.c

******************************************************************/

#include "defines.h"
#include "state_construct.h"
#include "cb_construct.h"
#include "index_conv_dec.h"
#include "do_plc.h"
#include "constants.h"
#include "enhancer_interface.h"
#include "xcorr_coef.h"
#include "lsf_check.h"
#include <stdio.h>
/*----------------------------------------------------------------*
 *  frame residual decoder function (subrutine to iLBC_decode)
 *---------------------------------------------------------------*/

void WebRtcIlbcfix_DecodeResidual(
    int16_t *memVec,
    int16_t *reverseDecresidual,
    int16_t *idxVec,
    int16_t *cb_index,
    int16_t *gain_index,
    int16_t *decresidual,  /* (o) decoded residual frame */
    int16_t *syntdenum,   /* (i) the decoded synthesis filter
                                  coefficients */
    int16_t state_short_len,
    int16_t nsub,
    int16_t state_first,
    int16_t startIdx,
    int16_t idxForMax
                                  ) {
  int16_t meml_gotten, Nfor, Nback, diff, start_pos;
  int16_t subcount, subframe;
  int16_t *mem = &memVec[CB_HALFFILTERLEN];   /* Memory for codebook */

  diff = STATE_LEN - state_short_len;

  if (state_first == 1) {
    start_pos = (startIdx-1)*SUBL;
  } else {
    start_pos = (startIdx-1)*SUBL + diff;
  }

  /* decode scalar part of start state */

  WebRtcIlbcfix_StateConstruct(idxForMax,
                               idxVec, &syntdenum[(startIdx-1)*(LPC_FILTERORDER+1)],
                               &decresidual[start_pos], state_short_len
                               );

  if (state_first==1) { // put adaptive part in the end 

    // setup memory

    WebRtcSpl_MemSetW16(mem, 0, (int16_t)(CB_MEML-state_short_len));
    WEBRTC_SPL_MEMCPY_W16(mem+CB_MEML-state_short_len, decresidual+start_pos,
                          state_short_len);

    // construct decoded vector 

    WebRtcIlbcfix_CbConstruct(
        &decresidual[start_pos+state_short_len],
        cb_index, gain_index,
        mem+CB_MEML-ST_MEM_L_TBL,
        ST_MEM_L_TBL, (int16_t)diff
                              );

  }
  else {// put adaptive part in the beginning

    // setup memory 

    meml_gotten = state_short_len;
    WebRtcSpl_MemCpyReversedOrder(mem+CB_MEML-1,
                                  decresidual+start_pos, meml_gotten);
    WebRtcSpl_MemSetW16(mem, 0, (int16_t)(CB_MEML-meml_gotten));

    // construct decoded vector

    WebRtcIlbcfix_CbConstruct(
        reverseDecresidual,
        cb_index, gain_index,
        mem+CB_MEML-ST_MEM_L_TBL,
        ST_MEM_L_TBL, diff
                              );

    // get decoded residual from reversed vector 

    WebRtcSpl_MemCpyReversedOrder(&decresidual[start_pos-1],
                                  reverseDecresidual, diff);
  }

  /* counter for predicted subframes */

  subcount=1;

  /* forward prediction of subframes */

  Nfor = nsub-startIdx-1;

  if( Nfor > 0 ) {

    // setup memory
    WebRtcSpl_MemSetW16(mem, 0, CB_MEML-STATE_LEN);
    WEBRTC_SPL_MEMCPY_W16(mem+CB_MEML-STATE_LEN,
                          decresidual+(startIdx-1)*SUBL, STATE_LEN);

    // loop over subframes to encode

    for (subframe=0; subframe<Nfor; subframe++) {
      // construct decoded vector
      WebRtcIlbcfix_CbConstruct(
          &decresidual[(startIdx+1+subframe)*SUBL],
          cb_index+subcount*CB_NSTAGES,
          gain_index+subcount*CB_NSTAGES,
          mem, MEM_LF_TBL, SUBL
                                );

      // update memory
      WEBRTC_SPL_MEMMOVE_W16(mem, mem+SUBL, CB_MEML-SUBL);
      WEBRTC_SPL_MEMCPY_W16(mem+CB_MEML-SUBL,
                            &decresidual[(startIdx+1+subframe)*SUBL], SUBL);

      subcount++;
    }
  }

  // backward prediction of subframes

  Nback = startIdx-1;

  if( Nback > 0 ){

    // setup memory

    meml_gotten = SUBL*(nsub+1-startIdx);
    if( meml_gotten > CB_MEML ) {
      meml_gotten=CB_MEML;
    }

    WebRtcSpl_MemCpyReversedOrder(mem+CB_MEML-1,
                                  decresidual+(startIdx-1)*SUBL, meml_gotten);
    WebRtcSpl_MemSetW16(mem, 0, (int16_t)(CB_MEML-meml_gotten));

    // loop over subframes to decode

    for (subframe=0; subframe<Nback; subframe++) {

      // construct decoded vector 
      WebRtcIlbcfix_CbConstruct(
          &reverseDecresidual[subframe*SUBL],
          cb_index+subcount*CB_NSTAGES,
          gain_index+subcount*CB_NSTAGES,
          mem, MEM_LF_TBL, SUBL
                                );

      // update memory
      WEBRTC_SPL_MEMMOVE_W16(mem, mem+SUBL, CB_MEML-SUBL);
      WEBRTC_SPL_MEMCPY_W16(mem+CB_MEML-SUBL,
                            &reverseDecresidual[subframe*SUBL], SUBL);

      subcount++;
    }

    // get decoded residual from reversed vector 
    WebRtcSpl_MemCpyReversedOrder(decresidual+SUBL*Nback-1,
                                  reverseDecresidual, SUBL*Nback);
  }
}
